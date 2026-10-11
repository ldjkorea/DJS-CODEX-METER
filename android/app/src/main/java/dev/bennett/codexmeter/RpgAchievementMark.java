package dev.bennett.codexmeter;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small original vector motifs, one paint/path and no bitmap allocations per badge. */
final class RpgAchievementMark extends Drawable {
    private final RpgCatalog.Entry entry;private final Paint p=new Paint(3);private final Path path=new Path();private final boolean dark;
    RpgAchievementMark(RpgCatalog.Entry e,boolean dark){entry=e;this.dark=dark;}
    public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/64f,b.height()/64f);
        int color=entry.rarity.equals("mythic")?0xfff1a0ac:entry.rarity.equals("legendary")?0xffe8ce91:entry.rarity.equals("epic")?0xffc7a7f4:entry.rarity.equals("rare")?0xffaee4ee:0xffb7c4ce;
        if(!dark){float[] hsv=new float[3];Color.colorToHSV(color,hsv);hsv[2]=.38f;color=Color.HSVToColor(hsv);}
        p.setColor(color);p.setStrokeWidth(2.4f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        path.reset();path.moveTo(32,3);path.lineTo(57,17);path.lineTo(57,45);path.lineTo(32,61);path.lineTo(7,45);path.lineTo(7,17);path.close();c.drawPath(path,p);
        String kind=entry.metric;
        if(entry.days>0&&kind.equals("tokens")){path.reset();path.moveTo(14,24);path.lineTo(27,31);path.lineTo(32,20);path.lineTo(37,31);path.lineTo(50,24);path.lineTo(43,40);path.lineTo(35,37);path.lineTo(32,48);path.lineTo(29,37);path.lineTo(21,40);path.close();c.drawPath(path,p);}
        else if(kind.equals("tokens")){path.reset();path.moveTo(32,17);path.lineTo(44,30);path.lineTo(32,48);path.lineTo(20,30);path.close();c.drawPath(path,p);c.drawLine(20,30,44,30,p);c.drawLine(32,17,32,48,p);}
        else if(kind.equals("streak")){c.drawRoundRect(17,22,34,37,7,7,p);c.drawRoundRect(30,29,47,44,7,7,p);}
        else if(kind.equals("tier")){path.reset();path.moveTo(17,26);path.lineTo(22,39);path.lineTo(42,39);path.lineTo(47,26);path.lineTo(38,31);path.lineTo(32,19);path.lineTo(26,31);path.close();c.drawPath(path,p);c.drawLine(22,45,42,45,p);}
        else if(kind.equals("dawn")){c.drawArc(18,18,47,47,65,280,false,p);c.drawCircle(42,21,3,p);c.drawLine(23,44,43,44,p);}
        else if(kind.equals("weeks")||kind.equals("plans")){c.drawCircle(32,32,11,p);c.save();c.rotate(-30,32,32);c.drawOval(13,26,51,38,p);c.restore();c.drawCircle(46,21,3,p);}
        else if(kind.equals("utilization")){c.drawArc(17,20,47,50,180,180,false,p);c.drawLine(32,35,42,25,p);c.drawCircle(32,35,3,p);c.drawLine(19,43,45,43,p);}
        else if(kind.equals("heritage")){path.reset();path.moveTo(21,47);path.cubicTo(49,31,46,15,38,17);path.cubicTo(20,18,19,31,21,47);c.drawPath(path,p);c.drawLine(21,47,38,23,p);c.drawLine(26,37,37,37,p);}
        else{c.drawCircle(32,32,9,p);for(int i=0;i<8;i++){double angle=i*Math.PI/4;c.drawLine(32+(float)Math.cos(angle)*14,32+(float)Math.sin(angle)*14,32+(float)Math.cos(angle)*18,32+(float)Math.sin(angle)*18,p);}if(kind.equals("weekend"))c.drawLine(18,48,46,48,p);}
        c.restore();
    }
    public void setAlpha(int value){p.setAlpha(value);invalidateSelf();}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
