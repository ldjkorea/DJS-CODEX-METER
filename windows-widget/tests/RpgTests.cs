using System;using System.Collections.Generic;using System.IO;using System.Linq;
namespace CodexMeterWidget {
 public static class RpgTests {
  static int checks;static void Check(bool v,string name){checks++;if(!v)throw new Exception(name);}
  public static int Main(){try{
   Check(!RpgTokenReader.IsCli(@"C:\Programs\Codex\Codex.exe"),"Never launch desktop GUI as app-server");Check(RpgTokenReader.IsCli(@"C:\Users\fixture\AppData\Local\OpenAI\Codex\bin\2e5e00daee91c61d\codex.exe"),"Observed desktop bundled CLI path supported");
   var result=Json.Read("{\"summary\":{\"lifetimeTokens\":2733583253,\"peakDailyTokens\":735977532,\"private\":\"NOT_EXPORTED\"},\"dailyUsageBuckets\":[{\"startDate\":\"2026-10-09\",\"tokens\":735977532,\"prompt\":\"NOT_EXPORTED\"}]}");
   var projected=RpgTokenReader.Project("synthetic-A",result,Clock.Now);Check(Json.Text(projected,"account")==LocalStore.AccountKey("synthetic-A"),"Hashed account");Check(!Json.Write(projected).Contains("NOT_EXPORTED"),"Whitelisted metadata only");Check(Json.Integer(Json.Object(projected,"summary"),"lifetimeTokens")==2733583253L,"64-bit tokens");
   Check(RpgTokenReader.Project("A",Json.Read("{\"summary\":null}"),Clock.Now)==null,"Unknown summary");
   bool bad=false;try{RpgTokenReader.Project("A",Json.Read("{\"summary\":{\"lifetimeTokens\":-1}}"),Clock.Now);}catch{bad=true;}Check(bad,"Negative tokens rejected");
   string dir=Path.Combine(Path.GetTempPath(),"gpt-hud-rpg-fixture-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(dir);
   string at=DateTimeOffset.UtcNow.AddSeconds(-1).ToString("o");string header="{\"type\":\"session_meta\",\"payload\":{\"id\":\"fixture-session\",\"creator_account_id\":\"A\",\"base_instructions\":\"NOT_EXPORTED\"}}\n";
   string token="{\"timestamp\":\""+at+"\",\"type\":\"event_msg\",\"payload\":{\"type\":\"token_count\",\"info\":{\"total_token_usage\":{\"input_tokens\":1000,\"cached_input_tokens\":800,\"cache_write_input_tokens\":5,\"output_tokens\":50,\"reasoning_output_tokens\":20,\"total_tokens\":1050},\"last_token_usage\":{\"input_tokens\":100,\"cached_input_tokens\":80,\"output_tokens\":5,\"reasoning_output_tokens\":2,\"total_tokens\":105}}}}\n";
   File.WriteAllText(Path.Combine(dir,"rollout-fixture.jsonl"),header+"{\"type\":\"event_msg\",\"payload\":{\"type\":\"task_started\",\"turn_id\":\"fixture-turn\"}}\n"+token+token+"{broken");
   var rows=RpgTokenReader.LocalMetadata(dir,"A");Check(rows.Count==1,"Repeated token notification projects latest cumulative only");Check(Json.Integer(Json.Object(rows[0],"total"),"totalTokens")==1050,"Raw counters preserved");Check(Json.Integer(Json.Object(rows[0],"total"),"cacheWriteInputTokens")==5,"Observed cache-write field preserved");Check(Json.Text(rows[0],"turn")=="fixture-turn","Actual turn identity");Check(!Json.Write(rows).Contains("NOT_EXPORTED"),"No instructions/transcripts exported");Check(RpgTokenReader.LocalMetadata(dir,"B").Count==0,"Creator account mismatch excluded");
   File.WriteAllText(Path.Combine(dir,"rollout-fixture.jsonl"),header.Replace("\"base_instructions\"","\"parent_thread_id\"")+token);Check(RpgTokenReader.LocalMetadata(dir,"A").Count==0,"Inherited subagent histories excluded");
   Console.WriteLine("RPG Windows metadata: "+checks+" checks passed; synthetic files only, no Codex login/network.");return 0;
  }catch(Exception e){Console.Error.WriteLine(e);return 1;}}
 }
}
