from pathlib import Path
import os,subprocess
if os.name!='nt':
 print('Windows RPG metadata/render tests require Windows; executed by the separate Windows CI job.')
 raise SystemExit(0)
root=Path(__file__).resolve().parents[1];repo=root.parent;out=root/'build/rpg2-windows';out.mkdir(parents=True,exist_ok=True)
compiler=Path(os.environ['WINDIR'])/'Microsoft.NET/Framework64/v4.0.30319/csc.exe'
sources=list((repo/'windows-widget/src').glob('*.cs'))
refs=['/reference:'+r+'.dll' for r in ['System','System.Core','System.Drawing','System.Windows.Forms','System.Web.Extensions','System.Security','System.Net.Http']]
resources=['/resource:'+str(repo/f'windows-widget/assets/prestige_{i}.png')+f',prestige_{i}' for i in range(10)]
for name,main in [('RpgTests','CodexMeterWidget.RpgTests'),('Tests','CodexMeterWidget.Tests')]:
 exe=out/(name+'.exe');subprocess.run([str(compiler),'/nologo','/codepage:65001','/target:exe','/main:'+main,'/out:'+str(exe),*refs,*resources,*map(str,sources),str(repo/f'windows-widget/tests/{name}.cs')],check=True)
 subprocess.run([str(exe),str(out/'previews')],check=True,timeout=90)
