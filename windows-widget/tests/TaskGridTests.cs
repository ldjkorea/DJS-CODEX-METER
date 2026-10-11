using System;using System.IO;using System.Linq;
namespace CodexMeterWidget {
 public static class TaskGridTests {
  public static void Main(string[] args){string folder=Path.Combine(Path.GetTempPath(),"hud-task-grid-"+Guid.NewGuid().ToString("N"));Directory.CreateDirectory(folder);
   using(var observer=new TaskObserve(folder,id=>"GPT HUD task "+id.Substring(0,6))){long now=Clock.Now;
    for(int i=0;i<8;i++)observer.Apply(i.ToString("x32"),(i+20).ToString("x32"),"task_started",now);
    TaskView[] rows=(TaskView[])observer.Snapshot()["rows"];if(rows.Length!=6||rows[0].Thread!=7.ToString("x32")||rows[5].Thread!=2.ToString("x32"))throw new Exception("Six latest tasks required");
    observer.Apply(3.ToString("x32"),23.ToString("x32"),"task_complete",now+1);
    rows=(TaskView[])observer.Snapshot()["rows"];if(rows.Length!=6||rows[0].State!="completed"||rows[0].Thread!=3.ToString("x32"))throw new Exception("Fourth-sixth task changes must remain visible");
    File.WriteAllText(args[0],Json.Write(observer.Snapshot()));Console.WriteLine("Windows six-task snapshot/order/change/serialization: passed (synthetic, no LAN/Tailscale call).");
   }
  }
 }
}
