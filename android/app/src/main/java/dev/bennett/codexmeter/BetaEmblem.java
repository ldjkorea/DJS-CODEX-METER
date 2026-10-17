package dev.bennett.codexmeter;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Each stable collectible retains its silhouette even before it is earned. */
final class BetaEmblem extends Drawable {
    final Paint p=new Paint(3);final String id;final boolean owned,dark;
    BetaEmblem(String id,boolean owned,boolean dark){this.id=id;this.owned=owned;this.dark=dark;}
    public void draw(Canvas c){Rect b=getBounds();c.save();c.translate(b.left,b.top);c.scale(b.width()/64f,b.height()/64f);String[] item=BetaCatalog.find(id);String rarity=item==null?"common":item[6];int color=!owned?(dark?0xffb0bbc8:0xff5a687a):BetaMedalUi.rarity(rarity);
        p.setStyle(Paint.Style.FILL);p.setColor(dark?0xff283240:0xffe4eaf3);c.drawCircle(30,30,27,p);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);c.drawCircle(30,30,25,p);
        if(item!=null&&item.length>7){String metric=item[7];int target=Integer.parseInt(item[8]);
            if(metric.equals("level")){Path gem=new Path();gem.moveTo(30,12);gem.lineTo(47,29);gem.lineTo(30,47);gem.lineTo(13,29);gem.close();c.drawPath(gem,p);}
            else if(metric.equals("days")){c.drawRoundRect(14,16,46,44,4,4,p);c.drawLine(14,24,46,24,p);c.drawLine(22,12,22,19,p);c.drawLine(38,12,38,19,p);}
            else if(metric.equals("perfect")){Path shield=new Path();shield.moveTo(16,17);shield.lineTo(44,17);shield.lineTo(41,38);shield.lineTo(30,47);shield.lineTo(19,38);shield.close();c.drawPath(shield,p);}
            else if(metric.equals("weeks")){c.drawArc(13,13,47,47,25,310,false,p);c.drawLine(16,17,17,27,p);c.drawLine(17,27,27,25,p);}
            else{c.drawCircle(30,30,16,p);for(int i=0;i<6;i++){c.save();c.rotate(i*60,30,30);c.drawLine(30,9,30,13,p);c.restore();}}
            p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(target>=100?13:16);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));c.drawText(Integer.toString(target),30,36,p);p.setStyle(Paint.Style.STROKE);
            if(BetaCatalog.title(id)){c.drawLine(22,48,38,48,p);c.drawLine(24,46,24,48,p);c.drawLine(30,44,30,48,p);c.drawLine(36,46,36,48,p);}
        }else if(BetaCatalog.title(id)){Path crown=new Path();crown.moveTo(15,22);crown.lineTo(20,40);crown.lineTo(40,40);crown.lineTo(45,22);crown.lineTo(36,28);crown.lineTo(30,16);crown.lineTo(24,28);crown.close();c.drawPath(crown,p);c.drawLine(20,44,40,44,p);int marks=id.equals("rpg3.starter")?1:id.equals("rpg3.title_rhythm")?2:id.equals("rpg3.workbench")?3:id.equals("rpg3.title_orbit")?4:5;for(int i=0;i<marks;i++)c.drawCircle(21+i*4.5f,47,1,p);}
        else if(id.contains("frame")||id.contains("annual")){c.drawCircle(30,30,id.contains("frame")?13:8,p);c.save();c.rotate(-30,30,30);c.drawOval(11,22,49,38,p);c.restore();if(id.contains("frame")){Path star=new Path();star.moveTo(30,16);star.lineTo(43,30);star.lineTo(30,44);star.lineTo(17,30);star.close();c.drawPath(star,p);}if(id.endsWith("3")||id.endsWith("50"))c.drawCircle(44,15,3,p);}
        else if(id.contains("gold")){int n=id.endsWith("6")?6:3;for(int i=0;i<n;i++){double a=i*Math.PI*2/n;c.drawCircle(30+(float)Math.cos(a)*13,30+(float)Math.sin(a)*13,5,p);}c.drawCircle(30,30,3,p);}
        else {c.drawRoundRect(16,18,44,43,3,3,p);c.drawLine(16,25,44,25,p);c.drawLine(23,14,23,21,p);c.drawLine(37,14,37,21,p);if(id.contains("four_weeks")||id.contains("weekly_4")){for(int i=0;i<4;i++)c.drawRoundRect(21+i%2*10,29+i/2*8,26+i%2*10,33+i/2*8,1,1,p);}else if(id.contains("daily_7")){for(int i=0;i<7;i++)c.drawCircle(22+i%4*5,30+i/4*7,1,p);}else{c.drawLine(22,34,28,39,p);c.drawLine(28,39,39,29,p);if(id.contains("first_week"))c.drawLine(20,47,40,47,p);}}
        if(!owned){p.setStyle(Paint.Style.FILL);p.setColor(dark?0xff18202b:0xfffbfcff);c.drawCircle(51,51,11,p);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);c.drawRoundRect(45,49,57,57,2,2,p);c.drawArc(47,43,55,53,180,180,false,p);}
        c.restore();}
    public void setAlpha(int alpha){p.setAlpha(alpha);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
