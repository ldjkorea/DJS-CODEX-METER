using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;

namespace CodexMeterWidget {
    // Metadata only. Keep historical protocol v1 and exact-retry idempotence.
    internal static class LanHistory {
        internal static long Integer(Dictionary<string,object> value,string key) {
            object raw;decimal n;if(!value.TryGetValue(key,out raw)||raw==null||raw is bool||!decimal.TryParse(Convert.ToString(raw,CultureInfo.InvariantCulture),NumberStyles.Float,CultureInfo.InvariantCulture,out n)||n!=decimal.Truncate(n)||n<long.MinValue||n>long.MaxValue)throw new InvalidDataException("Invalid integer metadata");
            return (long)n;
        }
        internal static object[] Records(Dictionary<string,object> request,string key,bool daily) {
            object input;if(!request.TryGetValue(key,out input))return new object[0];
            var rows=LanSync.JsonCodec.Deserialize<List<Dictionary<string,object>>>(LanSync.JsonCodec.Serialize(input));
            if(rows==null||rows.Count>100000)throw new InvalidDataException("Invalid history metadata");
            var result=new List<object>();foreach(var row in rows){if(row==null)throw new InvalidDataException("Invalid history row");var safe=new Dictionary<string,object>();
                if(daily){string day=Json.Text(row,"day");DateTime parsed;double points;
                    if(!DateTime.TryParseExact(day,"yyyy-MM-dd",CultureInfo.InvariantCulture,DateTimeStyles.None,out parsed)||!double.TryParse(Json.Text(row,"points"),NumberStyles.Float,CultureInfo.InvariantCulture,out points)||double.IsNaN(points)||double.IsInfinity(points)||points<0)throw new InvalidDataException("Invalid daily evidence");
                    safe["day"]=day;safe["points"]=points;
                    foreach(string field in new[]{"observations","first","last","covered_ms","uncertain","boundaries"})if(row.ContainsKey(field)){long n=Integer(row,field);if(n<0||(field=="first"||field=="last")&&n>Clock.Now+300000)throw new InvalidDataException("Invalid daily scope");safe[field]=n;}
                    if(row.ContainsKey("policy")){string policy=Json.Text(row,"policy");if(!Regex.IsMatch(policy,@"^[a-z0-9_:.-]{1,120}\|[a-z0-9_-]{1,40}\|[0-9]{1,8}$"))throw new InvalidDataException("Invalid daily policy");safe["policy"]=policy;}
                    if(row.ContainsKey("legacy")){if(!(row["legacy"] is bool))throw new InvalidDataException("Invalid legacy flag");safe["legacy"]=row["legacy"];}
                }else{string type=Json.Text(row,"type");long at=Integer(row,"at");if(!new[]{"credit_used","BASELINE","NORMAL","RESET","EARLY_WINDOW","CREDIT_CHANGE","CORRECTION","POLICY_CHANGE","GAP","MISSING_TIMELINE"}.Contains(type)||at<=0||at>Clock.Now+300000)throw new InvalidDataException("Invalid event evidence");safe["type"]=type;safe["at"]=at;
                    if(row.ContainsKey("meter")){string meter=Json.Text(row,"meter");if(!Regex.IsMatch(meter,@"^[a-z0-9_:.-]{0,120}$"))throw new InvalidDataException("Invalid event meter");safe["meter"]=meter;}
                    if(row.ContainsKey("origin")){string origin=Json.Text(row,"origin");if(!new[]{"confirmed","observed","unknown","legacy"}.Contains(origin))throw new InvalidDataException("Invalid event origin");safe["origin"]=origin;}
                }result.Add(safe);
            }return result.ToArray();
        }
        internal static Observation[] Observations(Observation[] rows) {
            foreach(var row in rows){double precise;
                if(row.Decimal==null)row.Decimal=row.Used.ToString("R",CultureInfo.InvariantCulture);
                if(row.Decimal.Length>80||!double.TryParse(row.Decimal,NumberStyles.Float,CultureInfo.InvariantCulture,out precise)||precise!=row.Used)throw new InvalidDataException("Invalid raw precision");
                row.Source=row.Source=="api_precise"||row.Source=="api_rounded"?row.Source:"legacy";
            }return rows;
        }
        private static object Canonical(object value) {
            var map=value as Dictionary<string,object>;if(map!=null)return map.OrderBy(x=>x.Key,StringComparer.Ordinal).ToDictionary(x=>x.Key,x=>Canonical(x.Value));
            var rows=value as object[];return rows==null?value:rows.Select(Canonical).ToArray();
        }
        internal static void GuardPush(string directory,Dictionary<string,object> request) {
            string path=Path.Combine(directory,"push-nonces.json"),id=Json.Text(request,"id");long issued=Integer(request,"at");
            var saved=File.Exists(path)?LanSync.JsonCodec.Deserialize<Dictionary<string,Dictionary<string,object>>>(File.ReadAllText(path)):new Dictionary<string,Dictionary<string,object>>();
            if(saved==null)throw new InvalidDataException("Preserve invalid nonce journal");
            foreach(string old in saved.Keys.ToArray())if(Integer(saved[old],"issued")<Clock.Now-300000)saved.Remove(old);
            var wire=LanSync.JsonCodec.Deserialize<Dictionary<string,object>>(LanSync.JsonCodec.Serialize(request));wire.Remove("secret");string digest;
            using(var sha=SHA256.Create())digest=BitConverter.ToString(sha.ComputeHash(Encoding.UTF8.GetBytes(LanSync.JsonCodec.Serialize(Canonical(wire))))).Replace("-","").ToLowerInvariant();
            Dictionary<string,object> prior;if(saved.TryGetValue(id,out prior)){if(Json.Text(prior,"digest")!=digest||Integer(prior,"issued")!=issued)throw new InvalidDataException("Changed push replay");return;}
            if(saved.Count>=2048)throw new InvalidDataException("Too many recent pushes");
            saved[id]=new Dictionary<string,object>{{"issued",issued},{"digest",digest}};
            // Durable before side effects: an exact retry can safely complete an interrupted merge.
            LocalStore.AtomicWrite(path,Encoding.UTF8.GetBytes(LanSync.JsonCodec.Serialize(saved)));
        }
    }
}
