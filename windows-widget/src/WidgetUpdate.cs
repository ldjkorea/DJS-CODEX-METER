using System;
using System.Collections;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Linq;
using System.Net.Http;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace CodexMeterWidget {
    // Uses only the public release channel, never account credentials or pairing data.
    public sealed class WidgetUpdate {
        public string Version, Name, Url, ChecksumsUrl;
        public long Size;
        public static bool AllowedUrl(string value) {
            return value!=null && Regex.IsMatch(value,@"^https://github\.com/ldjkorea/(?:DJS-GPT-HUD|DJS-CODEX-METER|codex-meter-ko)/releases/download/[^/]+/[^/?#]+$");
        }
        public static WidgetUpdate Parse(string json) {
            var root=Json.Read(json);
            if(Convert.ToBoolean(root["draft"])||Convert.ToBoolean(root["prerelease"]))return null;
            WidgetUpdate found=null;string sums=null;
            foreach(var item in (IEnumerable)root["assets"]){
                var asset=(System.Collections.Generic.Dictionary<string,object>)item;
                string name=Convert.ToString(asset["name"]),url=Convert.ToString(asset["browser_download_url"]);
                if(!AllowedUrl(url))continue;
                if(name=="SHA256SUMS.txt")sums=url;
                var match=Regex.Match(name,@"^CodexMeter-WindowsWidget-(\d+\.\d+\.\d+)\.zip$");
                if(match.Success){var v=new System.Version(match.Groups[1].Value);
                    if(found==null||v>new System.Version(found.Version))found=new WidgetUpdate{Version=v.ToString(),Name=name,Url=url,Size=Convert.ToInt64(asset["size"])};}
            }
            if(found==null||sums==null||found.Size<=0||found.Size>50000000)return null;
            found.ChecksumsUrl=sums;return found;
        }
        private static HttpClient Client(){var c=new HttpClient{Timeout=TimeSpan.FromMinutes(2)};c.DefaultRequestHeaders.UserAgent.ParseAdd("GPT-HUD-Windows/"+WidgetForm.Version);return c;}
        public static async Task<WidgetUpdate> Check(){using(var c=Client())return Parse(await c.GetStringAsync("https://api.github.com/repos/ldjkorea/DJS-GPT-HUD/releases/latest"));}
        public static bool HashMatches(byte[] bytes,string sums,string name){
            var matches=Regex.Matches(sums,@"(?m)^([a-fA-F0-9]{64})[ \t]+\*?"+Regex.Escape(name)+@"\r?$");
            if(matches.Count!=1)return false;
            using(var sha=SHA256.Create())return string.Equals(BitConverter.ToString(sha.ComputeHash(bytes)).Replace("-",""),matches[0].Groups[1].Value,StringComparison.OrdinalIgnoreCase);
        }
        public async Task<string> Prepare(){
            if(!AllowedUrl(Url)||!AllowedUrl(ChecksumsUrl))throw new InvalidDataException();
            byte[] bytes;string sums;using(var c=Client()){sums=await c.GetStringAsync(ChecksumsUrl);bytes=await c.GetByteArrayAsync(Url);}
            if(bytes.LongLength!=Size||!HashMatches(bytes,sums,Name))throw new InvalidDataException("다운로드 체크섬 불일치");
            string target=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"GPT-HUD-Widget-Updates",Version+"-"+Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(target);
            using(var input=new MemoryStream(bytes))using(var zip=new ZipArchive(input,ZipArchiveMode.Read)){
                long total=0;if(zip.Entries.Count>200)throw new InvalidDataException();
                foreach(var entry in zip.Entries){
                    total+=entry.Length;if(total>100000000)throw new InvalidDataException();
                    string path=Path.GetFullPath(Path.Combine(target,entry.FullName));
                    if(!path.StartsWith(target+Path.DirectorySeparatorChar,StringComparison.OrdinalIgnoreCase))throw new InvalidDataException();
                    if(string.IsNullOrEmpty(entry.Name)){Directory.CreateDirectory(path);continue;}
                    Directory.CreateDirectory(Path.GetDirectoryName(path));entry.ExtractToFile(path,false);
                }
            }
            string exe=Path.Combine(target,"CodexMeterWidget.exe");
            if(!File.Exists(exe)||FileVersionInfo.GetVersionInfo(exe).ProductVersion!=Version)throw new InvalidDataException("버전 검증 실패");
            return exe;
        }
        private static string Quote(string path){return "'"+path.Replace("'","''")+"'";}
        public static void RestartAfterExit(string exe){
            // Wait for normal shutdown to release the single-instance mutex and LAN port.
            string script=Path.Combine(Path.GetDirectoryName(exe),"restart.ps1");
            string body="$ErrorActionPreference='Stop'\n$old=Get-Process -Id "+Process.GetCurrentProcess().Id+" -ErrorAction SilentlyContinue\nif ($old -and !$old.WaitForExit(30000)) { exit 1 }\nStart-Sleep -Milliseconds 500\nStart-Process -FilePath "+Quote(exe)+" -WorkingDirectory "+Quote(Path.GetDirectoryName(exe))+"\n";
            File.WriteAllText(script,body,new UTF8Encoding(true));
            Process.Start(new ProcessStartInfo("powershell.exe","-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File \""+script+"\""){UseShellExecute=false,CreateNoWindow=true});
        }
    }
}
