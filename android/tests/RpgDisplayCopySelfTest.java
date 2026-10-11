package dev.bennett.codexmeter;
import java.util.*;
public final class RpgDisplayCopySelfTest {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args){
        Set<String> names=new HashSet<>();check(RpgCatalog.ALL.size()==53,"Existing achievement count");
        for(RpgCatalog.Entry e:RpgCatalog.ALL){
            String ko=RpgDisplayCopy.name(e,true),en=RpgDisplayCopy.name(e,false);
            check(!ko.equals(e.ko),"New presentation exists: "+e.id);
            check(names.add(ko),"Unique name: "+e.id);
            check(!en.isEmpty()&&!RpgDisplayCopy.flavor(e,true).isEmpty()&&!RpgDisplayCopy.condition(e,false).isEmpty(),"Bilingual copy: "+e.id);
            if(e.days>0)check(RpgDisplayCopy.condition(e,true).contains("활동 "+e.days+"일"),"Secondary days shown");
            if(e.weeks>0)check(RpgDisplayCopy.condition(e,true).contains("유효 주간 "+e.weeks+"회"),"Secondary weeks shown");
            check(RpgCollectionFacts.select(null,0,0,0,ko,false,Collections.emptyList()).stream().anyMatch(row->row.id.equals(e.id)),"Displayed name searchable");
            check(RpgCollectionFacts.select(null,0,0,0,e.ko,false,Collections.emptyList()).stream().anyMatch(row->row.id.equals(e.id)),"Original name still searchable");
        }
        System.out.println("RpgDisplayCopySelfTest: "+checks+" checks passed");
    }
}
