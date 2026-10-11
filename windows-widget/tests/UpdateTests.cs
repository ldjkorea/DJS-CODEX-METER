using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
namespace CodexMeterWidget {
    public static class UpdateTests {
        static int count;
        static void Check(bool pass,string name){count++;if(!pass)throw new Exception(name);}
        public static int Main(){try{
            System.Net.ServicePointManager.SecurityProtocol=System.Net.SecurityProtocolType.Tls12;
            string baseUrl="https://github.com/ldjkorea/codex-meter-ko/releases/download/v2.8.26/";
            Check(WidgetUpdate.AllowedUrl(baseUrl+"SHA256SUMS.txt"),"repository allowed");
            Check(!WidgetUpdate.AllowedUrl(baseUrl.Replace("github.com","github.com.evil.test")+"x"),"host rejected");
            Check(!WidgetUpdate.AllowedUrl(baseUrl.Replace("ldjkorea","other")+"x"),"other repository rejected");
            byte[] value=Encoding.UTF8.GetBytes("fixture");string hash;
            using(var sha=SHA256.Create())hash=BitConverter.ToString(sha.ComputeHash(value)).Replace("-","").ToLowerInvariant();
            Check(WidgetUpdate.HashMatches(value,hash+"  fixture.zip\n","fixture.zip"),"valid checksum");
            Check(!WidgetUpdate.HashMatches(value,new string('0',64)+"  fixture.zip\n","fixture.zip"),"corruption rejected");
            Check(!WidgetUpdate.HashMatches(value,hash+"  other.zip\n","fixture.zip"),"different asset rejected");
            Check(!WidgetUpdate.HashMatches(value,hash+"  fixture.zip\n"+hash+"  fixture.zip\n","fixture.zip"),"duplicate checksum rejected");
            var latest=WidgetUpdate.Check().GetAwaiter().GetResult();
            Check(latest!=null && latest.Version=="0.1.8","actual current public Windows release");
            Check(new Version(latest.Version)<new Version(WidgetForm.Version),"local newer version cannot downgrade");
            string exe=latest.Prepare().GetAwaiter().GetResult();
            Check(File.Exists(exe),"actual download checksum extraction and exe version validated");
            Console.WriteLine("Windows updater: "+count+" checks passed. Live public ZIP validated; no installer launched.");return 0;
        }catch(Exception e){Console.Error.WriteLine(e);return 1;}}
    }
}
