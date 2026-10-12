package dev.bennett.codexmeter;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.*;

/** Fixed, bounded PNG canvas. No EXIF, raw logs, or account fields. */
final class ProfileCardRenderer {
    static final int WIDTH=900,HEIGHT=1120;
    private ProfileCardRenderer(){}
    static String description(Context c,ProfileCard model){
        StringBuilder text=new StringBuilder(c.getString(R.string.profile_preview));
        text.append(" · ").append(model.tier<0?c.getString(R.string.fun_placing):TierPresentation.ENGLISH[model.tier]);
        text.append(" · RPG LEVEL · ").append(model.level==0?"—":model.level);
        if(!model.titleId.isEmpty())text.append(" · ").append(RpgGrowthUi.title(c,model.titleId));
        for(String id:model.medals)text.append(" · ").append(RpgGrowthUi.name(c,RpgGrowthFacts.entry(id)));
        return text.append(" · ").append(c.getString(R.string.profile_disclaimer)).toString();
    }
    static Bitmap render(Context c,ProfileCard model,boolean dark,boolean date){
        Bitmap image=Bitmap.createBitmap(WIDTH,HEIGHT,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(image);
        int bg=dark?0xff101214:0xfff3f4f5,fg=dark?0xffedf0f4:0xff1b2026,secondary=dark?0xffb8c0ca:0xff4d5866;
        canvas.drawColor(bg);Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(model.tier<0?secondary:TierTheme.COLORS[model.tier]);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);canvas.drawRoundRect(24,24,WIDTH-24,HEIGHT-24,32,32,p);p.setStyle(Paint.Style.FILL);
        Typeface regular=androidx.core.content.res.ResourcesCompat.getFont(c,R.font.pretendard_regular),bold=androidx.core.content.res.ResourcesCompat.getFont(c,R.font.pretendard_bold);
        text(canvas,"GPT HUD",bold,36,secondary,62,52,776);
        String name=model.nickname.isEmpty()?c.getString(R.string.profile_anonymous):model.nickname;
        text(canvas,name,bold,44,fg,62,110,776);
        if(model.tier>=0){Drawable crest=androidx.core.content.ContextCompat.getDrawable(c,TierTheme.EMBLEMS[model.tier]);if(crest!=null){crest=crest.mutate();crest.setBounds(300,205,600,505);crest.draw(canvas);}}
        else text(canvas,c.getString(R.string.fun_placing),bold,36,secondary,140,320,620);
        String rank=model.tier<0?"—":TierPresentation.ENGLISH[model.tier];
        text(canvas,rank,bold,46,fg,62,515,776);
        text(canvas,model.level==0?"RPG LEVEL · —":"RPG LEVEL · "+model.level,bold,32,fg,62,585,776);
        String title=model.titleId.isEmpty()?c.getString(R.string.rpg3_not_equipped):RpgGrowthUi.title(c,model.titleId);
        text(canvas,title,bold,32,fg,62,646,776);
        for(int i=0;i<model.medals.size();i++){
            RpgCatalog.Entry e=RpgGrowthFacts.entry(model.medals.get(i));int x=62+i*260;
            Drawable medal=new RpgAchievementMark(e,dark);medal.setBounds(x+60,740,x+180,860);medal.draw(canvas);
            text(canvas,RpgGrowthUi.name(c,e),bold,24,fg,x,877,240);
        }
        if(model.medals.isEmpty())text(canvas,c.getString(R.string.growth_showcase_empty),regular,28,secondary,62,794,776);
        text(canvas,c.getString(R.string.profile_disclaimer),regular,24,secondary,62,978,776);
        if(date)text(canvas,java.time.LocalDate.now(LedgerAggregation.ZONE).toString(),regular,22,secondary,62,1062,776);
        return image;
    }
    private static void text(Canvas canvas,String text,Typeface font,float size,int color,int x,int y,int width){
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTypeface(font);p.setTextSize(size);p.setColor(color);
        StaticLayout layout=StaticLayout.Builder.obtain(text,0,text.length(),p,width).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).build();
        canvas.save();canvas.translate(x,y);layout.draw(canvas);canvas.restore();
    }
}
