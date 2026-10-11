package dev.bennett.codexmeter;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Original vector world crest: globe, orbital star and laurel; no national emblem. */
final class WorldTierMark extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final int grade,color;private int alpha=255;
    WorldTierMark(int grade,boolean dark){this.grade=grade;color=RpgGrowthUi.rankAccent(grade,dark);}
    @Override public void draw(Canvas c){
        Rect b=getBounds();float size=Math.min(b.width(),b.height());if(size<=0)return;
        c.save();c.translate(b.centerX()-size/2,b.centerY()-size/2);c.scale(size/100,size/100);
        paint.setColor(color);paint.setAlpha(alpha);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeCap(Paint.Cap.ROUND);
        c.drawCircle(50,48,25,paint);c.drawOval(38,23,62,73,paint);c.drawOval(25,39,75,57,paint);
        c.drawLine(50,23,50,73,paint);c.drawLine(25,48,75,48,paint);
        c.save();c.rotate(-28,50,48);c.drawOval(15,34,85,62,paint);c.restore();
        c.drawArc(11,10,89,90,30,120,false,paint);c.drawArc(11,10,89,90,210,120,false,paint);
        for(int side:new int[]{-1,1})for(int i=0;i<5;i++){
            float x=50+side*(27+i*2),y=78-i*10;
            path.reset();path.moveTo(x,y);path.quadTo(x+side*14,y-5,x+side*8,y-12);path.quadTo(x,y-8,x,y);paint.setStyle(Paint.Style.FILL);paint.setAlpha(alpha*3/4);c.drawPath(path,paint);
        }
        paint.setAlpha(alpha);path.reset();path.moveTo(50,3);path.lineTo(54,11);path.lineTo(63,13);path.lineTo(56,19);path.lineTo(57,27);path.lineTo(50,23);path.lineTo(43,27);path.lineTo(44,19);path.lineTo(37,13);path.lineTo(46,11);path.close();c.drawPath(path,paint);
        for(int i=0;i<Math.max(1,(grade+2)/2);i++){float x=50+(i-(Math.max(1,(grade+2)/2)-1)/2f)*8;c.drawCircle(x,92,2,paint);}
        c.restore();
    }
    @Override public void setAlpha(int value){alpha=value;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter value){paint.setColorFilter(value);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
