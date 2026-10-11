package dev.bennett.codexmeter;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original deterministic metal medals. Catalog IDs select geometry, never random colors. */
final class RpgAchievementMark extends Drawable {
    private final RpgCatalog.Entry entry; private final Paint p=new Paint(3); private final Path path=new Path(); private final boolean dark; private int alpha=255; private final int[] motif; private final int color;
    RpgAchievementMark(RpgCatalog.Entry e,boolean dark){entry=e;this.dark=dark;motif=RpgCollectionFacts.motif(e);
        int tint=entry.rarity.equals("mythic")?0xffe9b7c1:entry.rarity.equals("legendary")?0xffddc79f:entry.rarity.equals("epic")?0xffc7b6e4:entry.rarity.equals("rare")?0xffb2d7df:0xffbdc7d0;
        if(!dark){float[] hsv=new float[3];Color.colorToHSV(tint,hsv);hsv[2]=.38f;tint=Color.HSVToColor(hsv);}
        color=tint;
    }
    public void draw(Canvas c){Rect b=getBounds();if(b.isEmpty())return;int frame=motif[0],symbol=motif[1],ornament=motif[2];
        c.save();c.translate(b.left,b.top);c.scale(b.width()/64f,b.height()/64f);
        p.setColor(color);p.setAlpha(alpha);p.setStrokeWidth(2.2f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        if(frame==0)c.drawCircle(32,32,28,p);
        else if(frame==1){path.reset();path.moveTo(32,3);path.lineTo(58,18);path.lineTo(54,46);path.lineTo(32,61);path.lineTo(10,46);path.lineTo(6,18);path.close();c.drawPath(path,p);}
        else if(frame==2){path.reset();path.moveTo(32,3);path.lineTo(60,32);path.lineTo(32,61);path.lineTo(4,32);path.close();c.drawPath(path,p);}
        else{c.drawRoundRect(8,6,56,58,12,12,p);c.drawLine(20,3,44,3,p);c.drawLine(20,61,44,61,p);}
        // Distinct central motifs: crystal, sun, flame, crown, moon, orbit and three-legged raven.
        if(symbol==0){path.reset();path.moveTo(32,15);path.lineTo(43,29);path.lineTo(32,47);path.lineTo(21,29);path.close();c.drawPath(path,p);c.drawLine(21,29,43,29,p);c.drawLine(32,15,32,47,p);}
        else if(symbol==1){c.drawCircle(32,32,9,p);for(int i=0;i<8;i++){double angle=i*Math.PI/4;c.drawLine(32+(float)Math.cos(angle)*13,32+(float)Math.sin(angle)*13,32+(float)Math.cos(angle)*18,32+(float)Math.sin(angle)*18,p);}}
        else if(symbol==2){path.reset();path.moveTo(32,15);path.cubicTo(30,27,18,28,23,40);path.cubicTo(25,49,41,49,42,36);path.cubicTo(41,28,36,26,36,21);path.lineTo(32,32);path.close();c.drawPath(path,p);c.drawLine(31,37,33,44,p);}
        else if(symbol==3){path.reset();path.moveTo(17,25);path.lineTo(22,40);path.lineTo(42,40);path.lineTo(47,25);path.lineTo(38,31);path.lineTo(32,18);path.lineTo(26,31);path.close();c.drawPath(path,p);c.drawLine(22,46,42,46,p);c.drawCircle(32,29,2,p);}
        else if(symbol==4){c.drawArc(18,17,46,46,65,280,false,p);c.drawCircle(42,21,3,p);c.drawLine(20,48,44,48,p);}
        else if(symbol==5){c.drawCircle(32,32,10,p);c.save();c.rotate(-30,32,32);c.drawOval(12,26,52,38,p);c.restore();c.drawCircle(46,20,3,p);}
        else{path.reset();path.moveTo(32,24);path.lineTo(17,18);path.lineTo(20,34);path.lineTo(29,36);path.lineTo(35,36);path.lineTo(44,34);path.lineTo(47,18);path.lineTo(32,24);c.drawPath(path,p);c.drawCircle(32,25,4,p);for(int x=26;x<=38;x+=6){c.drawLine(32,34,x,44,p);c.drawLine(x,44,x+3,46,p);}}
        // Frame-specific runes alter the center and side ornament, not just the hue.
        for(int i=0;i<=frame;i++){float x=24+i*5;c.drawLine(x,52,x+2,54,p);}
        if(ornament==0){c.drawLine(9,31,13,31,p);c.drawLine(51,31,55,31,p);}
        else{c.drawCircle(12,31,2,p);c.drawCircle(52,31,2,p);c.drawLine(28,10,36,10,p);}
        c.restore();
    }
    public void setAlpha(int value){alpha=value;invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);invalidateSelf();}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
