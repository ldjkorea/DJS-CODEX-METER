using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace CodexMeterWidget {
    // One official read per hour on demand. Never starts a turn, changes Codex settings, or exports auth.
    public static class RpgTokenReader {
        private static readonly object gate=new object();
        private static readonly Dictionary<string,Dictionary<string,object>> cache=new Dictionary<string,Dictionary<string,object>>();
        private static readonly Dictionary<string,long> attempts=new Dictionary<string,long>();
        private static bool busy;
        public static Dictionary<string,object> Snapshot(string account,string directory) {
            string key=LocalStore.AccountKey(account);lock(gate){Dictionary<string,object> value;cache.TryGetValue(key,out value);long tried;attempts.TryGetValue(key,out tried);
                if(!busy&&Clock.Now-tried>=3600000){busy=true;attempts[key]=Clock.Now;Task.Run(()=>{try{var result=Read(account);if(result!=null){lock(gate)cache[key]=result;LocalStore.AtomicWrite(Path.Combine(directory,"accounts",key,"rpg-tokens.json"),Encoding.UTF8.GetBytes(Json.Write(result)));}}catch{/* Last verified state remains; no auth or error body retained. */}finally{lock(gate)busy=false;}});}
                return value;
            }
        }
        private static string Home(){string home=Environment.GetEnvironmentVariable("CODEX_HOME");return string.IsNullOrEmpty(home)?Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),".codex"):home;}
        private static string Identity(){var root=Json.Read(File.ReadAllText(Path.Combine(Home(),"auth.json")));var tokens=Json.Object(root,"tokens");if(tokens==null)return "";return Tokens.AccountFromJwt(Json.Text(tokens,"id_token"));}
        private static string Binary(){
            string npm=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData),"npm","node_modules","@openai","codex","node_modules","@openai","codex-win32-x64","vendor","x86_64-pc-windows-msvc","bin","codex.exe");
            if(File.Exists(npm))return npm;
            foreach(var process in Process.GetProcessesByName("codex")){try{string path=process.MainModule.FileName;if(IsCli(path)&&File.Exists(path))return path;}catch{}finally{process.Dispose();}}
            return null;
        }
        internal static bool IsCli(string path){return path!=null&&System.Text.RegularExpressions.Regex.IsMatch(path.Replace('/','\\'),@"(?:\\OpenAI\\Codex\\bin\\[a-f0-9]+|\\resources|\\vendor\\x86_64-pc-windows-msvc\\bin)\\codex\.exe$",System.Text.RegularExpressions.RegexOptions.IgnoreCase);}
        public static Dictionary<string,object> Project(string account,Dictionary<string,object> result,long at){
            var summary=Json.Object(result,"summary");if(summary==null)return null;var safe=new Dictionary<string,object>();
            foreach(string field in new[]{"lifetimeTokens","peakDailyTokens","longestRunningTurnSec","currentStreakDays","longestStreakDays"}){object value;if(summary.TryGetValue(field,out value)&&value!=null){long count=Convert.ToInt64(value);if(count<0)throw new InvalidDataException();safe[field]=count;}}
            var days=new List<Dictionary<string,object>>();object incoming;
            if(result.TryGetValue("dailyUsageBuckets",out incoming)&&incoming!=null)foreach(object item in (System.Collections.IEnumerable)incoming){var d=item as Dictionary<string,object>;if(d==null)throw new InvalidDataException();string day=Json.Text(d,"startDate");DateTime parsed;long count=Json.Integer(d,"tokens",-1);if(!DateTime.TryParseExact(day,"yyyy-MM-dd",System.Globalization.CultureInfo.InvariantCulture,System.Globalization.DateTimeStyles.None,out parsed)||count<0||days.Count>=4000)throw new InvalidDataException();days.Add(new Dictionary<string,object>{{"startDate",day},{"tokens",count}});}
            return new Dictionary<string,object>{{"source","codex_account_usage_v1"},{"account",LocalStore.AccountKey(account)},{"at",at},{"summary",safe},{"dailyUsageBuckets",incoming==null?null:(object)days}};
        }
        private static Dictionary<string,object> Read(string account){
            string binary=Binary();if(binary==null||Identity()!=account)return null;
            using(var process=new Process{StartInfo=new ProcessStartInfo(binary,"app-server"){UseShellExecute=false,CreateNoWindow=true,RedirectStandardInput=true,RedirectStandardOutput=true,RedirectStandardError=true,StandardOutputEncoding=Encoding.UTF8}}){
                process.Start();process.BeginErrorReadLine();
                try{var read=Task.Run(()=>{
                    Call(process,1,"initialize",new Dictionary<string,object>{{"clientInfo",new Dictionary<string,object>{{"name","gpt_hud_rpg_readonly"},{"version","1"}}},{"capabilities",new Dictionary<string,object>{{"experimentalApi",true}}}});
                    process.StandardInput.WriteLine("{\"method\":\"initialized\"}");process.StandardInput.Flush();
                    var value=Call(process,2,"account/usage/read",null);if(Identity()!=account)return null;var result=Project(account,value,Clock.Now);
                    if(result!=null)result["local_rows"]=LocalMetadata(Path.Combine(Home(),"sessions"),account);return result;
                });if(!read.Wait(25000))return null;return read.Result;
                }finally{try{if(!process.HasExited)process.Kill();}catch{}}
            }
        }
        public static List<Dictionary<string,object>> LocalMetadata(string root,string account){
            var result=new List<Dictionary<string,object>>();if(!Directory.Exists(root))return result;
            foreach(var file in Directory.EnumerateFiles(root,"rollout-*.jsonl",SearchOption.AllDirectories).Select(p=>new FileInfo(p)).OrderByDescending(p=>p.LastWriteTimeUtc).Take(24)){
                try{using(var stream=new FileStream(file.FullName,FileMode.Open,FileAccess.Read,FileShare.ReadWrite|FileShare.Delete)){
                    Dictionary<string,object> meta;using(var head=new StreamReader(stream,Encoding.UTF8,true,4096,true)){string first=head.ReadLine();if(first==null||first.Length>256*1024)continue;var record=Json.Read(first);if(Json.Text(record,"type")!="session_meta")continue;meta=Json.Object(record,"payload");}
                    if(meta==null||Json.Text(meta,"creator_account_id")!=account||Json.Text(meta,"parent_thread_id").Length>0||Json.Object(meta,"history_base")!=null)continue;
                    string session=Json.Text(meta,"id"),turn="unknown";long offset=Math.Max(0,stream.Length-2*1024*1024);stream.Seek(offset,SeekOrigin.Begin);
                    Dictionary<string,object> latest=null;using(var reader=new StreamReader(stream,Encoding.UTF8,true,4096,true)){if(offset>0)reader.ReadLine();string line;
                        while((line=reader.ReadLine())!=null){if(line.Length>256*1024)continue;try{var entry=Json.Read(line);string type=Json.Text(entry,"type");var payload=Json.Object(entry,"payload");if(payload==null)continue;
                            if(type=="turn_context"&&Json.Text(payload,"turn_id").Length>0)turn=Json.Text(payload,"turn_id");
                            if(type!="event_msg")continue;string kind=Json.Text(payload,"type");if(kind=="task_started"&&Json.Text(payload,"turn_id").Length>0)turn=Json.Text(payload,"turn_id");
                            if(kind!="token_count")continue;var info=Json.Object(payload,"info");if(info==null)continue;var total=Breakdown(Json.Object(info,"total_token_usage"));var last=Breakdown(Json.Object(info,"last_token_usage"));DateTimeOffset at;
                            if(total==null||last==null||!DateTimeOffset.TryParse(Json.Text(entry,"timestamp"),out at)||at.ToUnixTimeMilliseconds()>Clock.Now)continue;
                            latest=new Dictionary<string,object>{{"account",LocalStore.AccountKey(account)},{"source","codex_thread_cumulative_v1"},{"identity_verified",true},{"session",session},{"turn",turn},{"at",at.ToUnixTimeMilliseconds()},{"total",total},{"last",last}};
                        }catch{/* Partial/unrecognized metadata is not activity. */}}
                    }if(latest!=null)result.Add(latest);
                }}catch{/* Never interrupt running Codex or guess an unassigned account. */}
            }return result;
        }
        private static Dictionary<string,object> Breakdown(Dictionary<string,object> raw){
            if(raw==null)return null;var value=new Dictionary<string,object>();
            string[] source={"input_tokens","cached_input_tokens","output_tokens","reasoning_output_tokens","total_tokens"},target={"inputTokens","cachedInputTokens","outputTokens","reasoningOutputTokens","totalTokens"};
            for(int i=0;i<source.Length;i++){long count=Json.Integer(raw,source[i],-1);if(count<0)return null;value[target[i]]=count;}
            if(Json.Integer(value,"cachedInputTokens")>Json.Integer(value,"inputTokens")||Json.Integer(value,"reasoningOutputTokens")>Json.Integer(value,"outputTokens")||checked(Json.Integer(value,"inputTokens")+Json.Integer(value,"outputTokens"))!=Json.Integer(value,"totalTokens"))return null;
            object cacheWrite;if(raw.TryGetValue("cache_write_input_tokens",out cacheWrite)&&cacheWrite!=null&&Convert.ToInt64(cacheWrite)>=0)value["cacheWriteInputTokens"]=Convert.ToInt64(cacheWrite);return value;
        }
        private static Dictionary<string,object> Call(Process process,int id,string method,object parameters){
            var request=new Dictionary<string,object>{{"id",id},{"method",method}};if(parameters!=null)request["params"]=parameters;
            process.StandardInput.WriteLine(Json.Write(request));process.StandardInput.Flush();
            for(int i=0;i<256;i++){string line=process.StandardOutput.ReadLine();if(line==null||line.Length>1024*1024)throw new InvalidDataException();var response=Json.Read(line);if(Json.Integer(response,"id",-1)!=id)continue;if(response.ContainsKey("error"))throw new InvalidDataException("Usage unavailable");return Json.Object(response,"result");}throw new InvalidDataException();
        }
    }
}
