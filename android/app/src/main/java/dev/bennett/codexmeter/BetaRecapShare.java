package dev.bennett.codexmeter;
import android.content.*;import android.graphics.*;import android.text.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

/** Explicit, local-only image generation. Opening the chooser is not proof of external delivery. */
final class BetaRecapShare {
    static void share(AppCompatActivity a,String account,String text){
        int accent=Ui.accent(a,true);
        BetaGrowthStore.WORKER.execute(()->{
            android.net.Uri uri=null;
            try{
                if(!BetaGrowthStore.current(a,account))return;
                TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(PretendardFont.semibold(a));paint.setTextSize(34);paint.setColor(0xffedf2fa);
                StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,700).setIncludePad(true).setLineSpacing(12,1.2f).build();
                Bitmap bitmap=Bitmap.createBitmap(840,Math.max(600,layout.getHeight()+180),Bitmap.Config.ARGB_8888);
                try{Canvas canvas=new Canvas(bitmap);canvas.drawColor(0xff11151c);Paint border=new Paint(3);border.setColor(accent);border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(4);canvas.drawRoundRect(24,24,816,bitmap.getHeight()-24,24,24,border);canvas.save();canvas.translate(70,80);layout.draw(canvas);canvas.restore();
                    java.io.File folder=new java.io.File(a.getCacheDir(),"profile-share");if(!folder.exists()&&!folder.mkdirs())throw new java.io.IOException();java.io.File file=new java.io.File(folder,java.util.UUID.randomUUID()+".png");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}uri=FileProvider.getUriForFile(a,a.getPackageName()+".profile.files",file);
                }finally{bitmap.recycle();}
            }catch(Exception ignored){}
            android.net.Uri image=uri;a.runOnUiThread(()->{if(!BetaUi.current(a,account))return;if(image==null){android.widget.Toast.makeText(a,BetaCopy.t(a,"failed"),android.widget.Toast.LENGTH_LONG).show();return;}
                Intent intent=new Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,image).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("GPT HUD recap",image));a.startActivity(Intent.createChooser(intent,BetaCopy.t(a,"share")));
            });
        });
    }
}
