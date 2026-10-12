package dev.bennett.codexmeter;

import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.text.*;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Recycled, account-scoped codex/wardrobe. All mutations use the existing stores. */
public final class RpgCollectionActivity extends AppCompatActivity {
    private static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    String mode,account=""; boolean dark,busy,initialized,draftReady,registered;
    JSONObject state; int status=-1,category,sort; String query=""; long generation;
    private String loadedSignature="",slotsSignature=""; private boolean reloadRequested;
    final List<String> draft=new ArrayList<>();
    RecyclerView list; GridLayoutManager layout; MedalAdapter adapter;
    LinearLayout header,slots; TextView count,goal,current; Spinner statusPicker,categoryPicker,sortPicker;
    AlertDialog dialog; Parcelable restoredLayout;
    private final BroadcastReceiver updates=new BroadcastReceiver(){public void onReceive(Context c,Intent i){load();}};
    boolean titles(){return mode.equals("titles");} boolean picker(){return mode.equals("showcase");}
    @Override protected void onCreate(Bundle saved){Ui.applySelectedTheme(this);super.onCreate(saved);dark=Ui.isDark(this);mode=getIntent().getStringExtra("collection_mode");if(!"titles".equals(mode)&&!"showcase".equals(mode))mode="achievements";
        if(saved!=null){status=saved.getInt("status",-1);category=saved.getInt("category");sort=saved.getInt("sort");query=saved.getString("query","");account=saved.getString("account","");ArrayList<String> ids=saved.getStringArrayList("draft");if(ids!=null){draft.addAll(ids);draftReady=saved.getBoolean("draft_ready");}restoredLayout=saved.getParcelable("list_position");}
        Ui.installPage(this,getString(titles()?R.string.medal_titles:picker()?R.string.medal_edit:R.string.medal_codex),true);
        androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh=findViewById(R.id.dashboard_refresh);refresh.setEnabled(false);
        View scroll=findViewById(R.id.dashboard_scroll);ViewGroup parent=(ViewGroup)scroll.getParent();int position=parent.indexOfChild(scroll);ViewGroup.LayoutParams params=scroll.getLayoutParams();parent.removeView(scroll);
        list=new RecyclerView(this);list.setTag("collection:list");list.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,24));list.setClipToPadding(false);list.setItemAnimator(null);
        int columns=titles()?1:getResources().getConfiguration().fontScale>1.25f||getResources().getConfiguration().screenWidthDp<300?1:2;
        layout=new GridLayoutManager(this,columns);list.setLayoutManager(layout);header=buildHeader();adapter=new MedalAdapter();list.setAdapter(adapter);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup(){public int getSpanSize(int i){return i==0||adapter.rows.isEmpty()?layout.getSpanCount():1;}});parent.addView(list,position,params);
    }
    @Override public boolean onSupportNavigateUp(){finish();return true;}
    @Override protected void onResume(){super.onResume();if(!registered){androidx.core.content.ContextCompat.registerReceiver(this,updates,new IntentFilter(AppConstants.ACTION_USAGE_UPDATED),androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);registered=true;}load();}
    @Override protected void onPause(){if(registered){unregisterReceiver(updates);registered=false;}super.onPause();}
    @Override protected void onDestroy(){generation++;if(dialog!=null)dialog.dismiss();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putInt("status",status);out.putInt("category",category);out.putInt("sort",sort);out.putString("query",query);out.putString("account",account);out.putStringArrayList("draft",new ArrayList<>(draft));out.putBoolean("draft_ready",draftReady);out.putParcelable("list_position",layout.onSaveInstanceState());}
    boolean valid(){return !isFinishing()&&!isDestroyed()&&!account.isEmpty()&&account.equals(SubscriptionStore.accountKey(this));}
    void load(){String key=SubscriptionStore.accountKey(this);if(key==null){clearAccount();return;}
        if(!account.equals(key)){clearAccount();account=key;}String fingerprint=CollectionPresentation.signature(this,key);
        if(!fingerprint.isEmpty()&&state!=null&&initialized&&fingerprint.equals(loadedSignature))return;
        if(busy){reloadRequested=true;return;}busy=true;long ticket=++generation;
        WORKER.execute(()->{JSONObject result=null;boolean error=false;try{JSONObject journal=RpgProgressStore.export(getApplicationContext(),key);result=journal==null?null:journal.optJSONObject("rpg");}catch(Exception e){error=true;}final JSONObject next=result;final boolean failed=error;
            runOnUiThread(()->{if(isDestroyed()||ticket!=generation)return;busy=false;if(!key.equals(SubscriptionStore.accountKey(this))){clearAccount();load();return;}
                if(failed){android.widget.Toast.makeText(this,R.string.rpg3_failed,Toast.LENGTH_SHORT).show();return;}
                try{state=titles()?TitleJourney.view(next):next;}catch(Exception e){state=next;}if(!draftReady&&picker()){draft.addAll(RpgShowcase.selected(this,state));draftReady=true;}
                if(status<0)status=titles()||picker()?RpgCollectionFacts.EARNED:RpgCollectionFacts.initial(state);
                initialized=false;statusPicker.setSelection(titles()?status==RpgCollectionFacts.EARNED?0:status==RpgCollectionFacts.ALL?1:2:status);categoryPicker.setSelection(category);sortPicker.setSelection(sort);initialized=true;
                loadedSignature=fingerprint;refresh(false);if(restoredLayout!=null){layout.onRestoreInstanceState(restoredLayout);restoredLayout=null;}
                if(reloadRequested){reloadRequested=false;load();}
            });});
    }
    void clearAccount(){generation++;busy=false;loadedSignature="";slotsSignature="";reloadRequested=false;if(dialog!=null)dialog.dismiss();account="";state=null;draft.clear();draftReady=false;status=-1;if(adapter!=null){adapter.update(Collections.emptyList());current.setText(R.string.rpg3_no_account);goal.setText(R.string.rpg3_no_account);count.setText("");if(slots!=null)slots.removeAllViews();}}
    LinearLayout buildHeader(){LinearLayout h=new LinearLayout(this);h.setOrientation(LinearLayout.VERTICAL);h.setPadding(Ui.dp(this,6),0,Ui.dp(this,6),Ui.dp(this,12));
        count=LedgerUi.caption(this,getString(R.string.next_loading),dark);h.addView(count);current=LedgerUi.heading(this,"",dark);goal=LedgerUi.caption(this,"",dark);
        if(titles()){RpgDisplayStyle.title(current,dark,22);h.addView(LedgerUi.caption(this,getString(R.string.medal_current_title),dark));h.addView(current);h.addView(LedgerUi.caption(this,getString(R.string.medal_title_hint),dark));
            Button remove=LedgerUi.action(this,getString(R.string.rpg3_unequip),false,dark,()->equip(""));remove.setTag("collection:unequip");h.addView(remove);Button random=LedgerUi.action(this,getString(R.string.medal_random),false,dark,()->{if(valid()){String id=RpgCollectionFacts.recommend(state,new Random());if(!id.isEmpty())detail(RpgGrowthFacts.titleEntry(id),true);}});random.setTag("collection:recommend");h.addView(random);
            EditText search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setTextColor(Ui.mainText(dark));search.setHintTextColor(Ui.secondaryText(dark));search.setHint(R.string.medal_search);search.setText(query);search.setContentDescription(getString(R.string.medal_search));search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int n,int after){}public void onTextChanged(CharSequence s,int st,int before,int n){query=s.toString();if(initialized)refresh(true);}public void afterTextChanged(Editable e){}});h.addView(search);
        }else{h.addView(goal);slots=new LinearLayout(this);slots.setOrientation(LinearLayout.HORIZONTAL);slots.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,14));h.addView(slots);
            if(picker()){h.addView(LedgerUi.caption(this,getString(R.string.medal_picker_hint),dark));h.addView(LedgerUi.action(this,getString(R.string.medal_equip_done),true,dark,this::saveShowcase));h.addView(LedgerUi.action(this,getString(android.R.string.cancel),false,dark,this::finish));}
            else h.addView(LedgerUi.action(this,getString(R.string.medal_edit),false,dark,()->open(this,"showcase")));
        }
        LinearLayout filters=new LinearLayout(this);filters.setOrientation(LinearLayout.HORIZONTAL);
        statusPicker=spinner(titles()?new int[]{R.string.medal_owned_titles,R.string.medal_all,R.string.medal_locked_titles}:new int[]{R.string.medal_all,R.string.medal_owned,R.string.medal_progress,R.string.medal_locked},R.string.medal_filter,value->{int next=titles()?new int[]{1,0,3}[value]:value;if(status!=next){status=next;refresh(true);}});
        categoryPicker=spinner(new int[]{R.string.medal_all,R.string.medal_tokens,R.string.medal_activity,R.string.medal_tier,R.string.medal_special},R.string.medal_category,value->{if(category!=value){category=value;refresh(true);}});
        if(!picker())h.addView(statusPicker,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout advanced=new LinearLayout(this);advanced.setOrientation(LinearLayout.VERTICAL);advanced.setVisibility(View.GONE);advanced.addView(categoryPicker,new LinearLayout.LayoutParams(-1,-2));h.addView(LedgerUi.action(this,getString(R.string.ref_more_filters),false,dark,()->advanced.setVisibility(advanced.getVisibility()==View.GONE?View.VISIBLE:View.GONE)));
        sortPicker=spinner(new int[]{R.string.medal_recommended,R.string.medal_near,R.string.medal_recent,R.string.medal_rare},R.string.medal_sort,value->{if(sort!=value){sort=value;refresh(true);}});advanced.addView(sortPicker,new LinearLayout.LayoutParams(-1,-2));h.addView(advanced);return h;
    }
    Spinner spinner(int[] resources,int label,java.util.function.IntConsumer select){Spinner s=new Spinner(this);String[] names=new String[resources.length];for(int i=0;i<names.length;i++)names[i]=getString(resources[i]);ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,names){@Override public View getView(int position,View recycled,ViewGroup parent){TextView text=(TextView)super.getView(position,recycled,parent);text.setSingleLine(false);text.setMaxLines(3);text.setEllipsize(null);text.setMinHeight(Ui.dp(RpgCollectionActivity.this,48));return text;}};a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setMinimumHeight(Ui.dp(this,48));s.setAdapter(a);s.setContentDescription(getString(label));s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){if(initialized)select.accept(i);}public void onNothingSelected(AdapterView<?> p){}});return s;}
    void refresh(boolean reset){if(adapter==null)return;List<String> selected=picker()?draft:RpgShowcase.selected(this,state);count.setText(getString(picker()?R.string.medal_slots:titles()?R.string.medal_title_count:R.string.growth_earned_count,picker()?draft.size():titles()?RpgTitleCatalog.owned(state).size():RpgGrowthFacts.earned(state).size(),titles()?RpgTitleCatalog.displayEntries().size():RpgCatalog.ALL.size()));
        current.setText(state==null||state.optString("equipped_title").isEmpty()?getString(R.string.rpg3_not_equipped):RpgGrowthUi.title(this,state.optString("equipped_title")));
        if(titles()){View random=header.findViewWithTag("collection:recommend"),remove=header.findViewWithTag("collection:unequip");if(random!=null)random.setEnabled(state!=null&&!RpgTitleCatalog.owned(state).isEmpty());if(remove!=null)remove.setEnabled(state!=null&&!state.optString("equipped_title").isEmpty());}
        if(!titles()){drawSlots(selected);List<RpgCatalog.Entry> goals=RpgCollectionFacts.select(state,2,0,0,"",false,selected);goal.setText(goals.isEmpty()?getString(R.string.medal_unknown):getString(R.string.medal_goal,RpgGrowthUi.name(this,goals.get(0))));}
        adapter.update(RpgCollectionFacts.select(state,picker()?1:Math.max(0,status),category,sort,query,titles(),selected));if(reset)list.scrollToPosition(0);
    }
    void drawSlots(List<String> selected){String fingerprint=CollectionPresentation.slots(this,state,selected);
        if(fingerprint.equals(slotsSignature)&&slots.getChildCount()==3)return;slotsSignature=fingerprint;slots.removeAllViews();for(int i=0;i<3;i++){
        LinearLayout cell;
        if(i<selected.size()){String id=selected.get(i);RpgCatalog.Entry e=RpgGrowthFacts.entry(id);cell=RpgDisplayStyle.medal(this,e,dark);if(picker())cell.setContentDescription(getString(R.string.medal_slot_remove,i+1,RpgGrowthUi.name(this,e)));cell.setOnClickListener(v->{if(picker()&&valid()){draft.remove(id);refresh(false);}else detail(e,false);});}
        else{cell=Ui.card(this,dark);cell.setGravity(Gravity.CENTER);cell.setMinimumHeight(Ui.dp(this,122));TextView empty=LedgerUi.caption(this,getString(R.string.medal_slot_empty,i+1),dark);empty.setGravity(Gravity.CENTER);cell.addView(empty);}
        RpgDisplayStyle.addMedal(slots,cell);
    }}
    void detail(RpgCatalog.Entry e,boolean title){if(!valid()||e==null)return;if(dialog!=null)dialog.dismiss();dialog=RpgMedalUi.show(this,state,e,dark,title,title?()->equip(e.id):null);}
    void equip(String id){if(!valid()||busy||state==null)return;busy=true;String key=account;WORKER.execute(()->{boolean saved=false;try{saved=RpgProgressStore.equip(getApplicationContext(),key,id);}catch(Exception ignored){}final boolean ok=saved;runOnUiThread(()->{busy=false;if(valid()&&key.equals(account)){Toast.makeText(this,ok?R.string.rpg3_saved:R.string.rpg3_failed,Toast.LENGTH_SHORT).show();load();}});});}
    void saveShowcase(){if(!valid()||busy||state==null||!draftReady)return;busy=true;String key=account;List<String> copy=new ArrayList<>(draft);JSONObject snapshot=state;WORKER.execute(()->{boolean saved=false;try{synchronized(UsageApi.NETWORK_LOCK){saved=RpgShowcase.save(getApplicationContext(),key,copy,snapshot);}}catch(Exception ignored){}final boolean ok=saved;runOnUiThread(()->{busy=false;if(valid()&&key.equals(account)){if(ok)finish();else Toast.makeText(this,R.string.rpg3_failed,Toast.LENGTH_SHORT).show();}});});}
    static void open(android.app.Activity a,String mode){a.startActivity(new Intent(a,RpgCollectionActivity.class).putExtra("collection_mode",mode));}
    final class MedalHolder extends RecyclerView.ViewHolder{final LinearLayout card;final ImageView icon;final TextView name,sub;final ProgressBar bar;
        MedalHolder(LinearLayout c){super(c);card=c;card.setGravity(titles()?Gravity.CENTER_VERTICAL:Gravity.CENTER_HORIZONTAL);card.setOrientation(titles()?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);card.setPadding(Ui.dp(RpgCollectionActivity.this,12),Ui.dp(RpgCollectionActivity.this,12),Ui.dp(RpgCollectionActivity.this,12),Ui.dp(RpgCollectionActivity.this,12));card.setMinimumHeight(Ui.dp(RpgCollectionActivity.this,titles()?64:160));
            icon=new ImageView(RpgCollectionActivity.this);icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);card.addView(icon,new LinearLayout.LayoutParams(Ui.dp(RpgCollectionActivity.this,titles()?32:64),Ui.dp(RpgCollectionActivity.this,titles()?32:64)));
            name=LedgerUi.caption(RpgCollectionActivity.this,"",dark);name.setTextSize(titles()?16:15);name.setTypeface(PretendardFont.semibold(RpgCollectionActivity.this));name.setTextColor(Ui.mainText(dark));name.setLineSpacing(Ui.dp(RpgCollectionActivity.this,2),1.05f);name.setGravity(titles()?Gravity.START:Gravity.CENTER);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(titles()?0:-1,-2,titles()?1:0);lp.setMarginStart(Ui.dp(RpgCollectionActivity.this,titles()?12:0));lp.topMargin=Ui.dp(RpgCollectionActivity.this,titles()?0:14);card.addView(name,lp);sub=LedgerUi.caption(RpgCollectionActivity.this,"",dark);sub.setTextSize(12);sub.setGravity(Gravity.CENTER);sub.setPadding(0,Ui.dp(RpgCollectionActivity.this,8),0,Ui.dp(RpgCollectionActivity.this,10));card.addView(sub);bar=Ui.progress(RpgCollectionActivity.this,dark);bar.setMax(1000);if(!titles())card.addView(bar,new LinearLayout.LayoutParams(-1,Ui.dp(RpgCollectionActivity.this,5)));
            if(titles()){sub.setMaxWidth(Ui.dp(RpgCollectionActivity.this,getResources().getConfiguration().fontScale>1.25f?112:84));LinearLayout.LayoutParams summary=new LinearLayout.LayoutParams(-2,-2);summary.setMarginStart(Ui.dp(RpgCollectionActivity.this,8));sub.setLayoutParams(summary);}
            RecyclerView.LayoutParams params=new RecyclerView.LayoutParams(-1,-2);params.setMargins(Ui.dp(RpgCollectionActivity.this,4),Ui.dp(RpgCollectionActivity.this,4),Ui.dp(RpgCollectionActivity.this,4),Ui.dp(RpgCollectionActivity.this,4));card.setLayoutParams(params);card.setFocusable(true);name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);sub.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);bar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }
    }
    final class Row {final RpgCatalog.Entry entry;final String stamp;Row(RpgCatalog.Entry e){entry=e;stamp=String.valueOf(RpgCollectionFacts.fact(state,e))+":"+RpgCollectionFacts.known(state,e)+":"+draft.contains(e.id)+":"+(state==null?"":state.optString("equipped_title"));}}
    final class MedalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{List<Row> rows=new ArrayList<>();MedalAdapter(){setHasStableIds(true);setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);}
        public int getItemCount(){return rows.isEmpty()?2:rows.size()+1;}public long getItemId(int p){return p==0?-1:rows.isEmpty()?-2:RpgCollectionFacts.index(rows.get(p-1).entry.id)+1;}public int getItemViewType(int p){return p==0?0:rows.isEmpty()?2:1;}
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p,int type){if(type==0)return new RecyclerView.ViewHolder(header){};if(type==2){TextView empty=LedgerUi.caption(RpgCollectionActivity.this,getString(titles()&&status==1?R.string.medal_no_owned:R.string.medal_empty),dark);empty.setPadding(0,Ui.dp(RpgCollectionActivity.this,20),0,Ui.dp(RpgCollectionActivity.this,20));return new RecyclerView.ViewHolder(empty){};}return new MedalHolder(Ui.card(RpgCollectionActivity.this,dark));}
        public void onBindViewHolder(RecyclerView.ViewHolder raw,int p){if(p==0)return;if(rows.isEmpty()){((TextView)raw.itemView).setText(getString(titles()&&status==1?R.string.medal_no_owned:R.string.medal_empty));return;}MedalHolder h=(MedalHolder)raw;RpgCatalog.Entry e=rows.get(p-1).entry;boolean earned=RpgCollectionFacts.earned(state,e);boolean chosen=picker()?draft.contains(e.id):titles()&&state!=null&&e.id.equals(state.optString("equipped_title"));
            h.name.setTextColor(titles()?TierTheme.accent(RpgCollectionActivity.this,dark):Ui.mainText(dark));if(!e.id.equals(h.icon.getTag())){h.icon.setImageDrawable(new RpgAchievementMark(e,dark));h.icon.setTag(e.id);}h.icon.setAlpha(earned?1:.65f);h.name.setText(titles()?RpgGrowthUi.title(RpgCollectionActivity.this,e.id):RpgGrowthUi.name(RpgCollectionActivity.this,e));h.sub.setText(getString(R.string.medal_summary,getString(!earned&&e.rarity.equals("mythic")?R.string.medal_mythic:RpgMedalUi.rarity(e)),chosen?getString(R.string.medal_equipped):titles()?getString(earned?R.string.medal_select:R.string.medal_locked):RpgMedalUi.status(RpgCollectionActivity.this,state,e)));h.bar.setProgress((int)(1000*Math.max(0,RpgCollectionFacts.ratio(state,e))));h.bar.setVisibility(!earned&&RpgCollectionFacts.ratio(state,e)>=0?View.VISIBLE:View.GONE);
            if(titles())h.sub.setText(h.sub.getText().toString().replace(" · ","\n"));
            android.graphics.drawable.GradientDrawable shape=LedgerUi.shape(RpgCollectionActivity.this,Ui.cardColor(RpgCollectionActivity.this,dark),18);shape.setStroke(Ui.dp(RpgCollectionActivity.this,chosen?2:1),chosen?TierTheme.accent(RpgCollectionActivity.this,dark):dark?0xff41464d:0xff939ba4);h.card.setBackground(shape);h.card.setTag("collection:medal:"+e.id);h.card.setContentDescription(h.name.getText()+" · "+h.sub.getText()+" · "+getString(picker()?R.string.medal_picker_hint:R.string.medal_details));
            h.card.setOnClickListener(v->{if(!valid()||busy)return;if(picker()){if(!RpgCollectionFacts.toggle(draft,e.id,state))Toast.makeText(RpgCollectionActivity.this,R.string.growth_showcase_limit,Toast.LENGTH_SHORT).show();refresh(false);}else detail(e,titles());});h.card.setOnLongClickListener(v->{detail(e,titles());return true;});
        }
        void update(List<RpgCatalog.Entry> entries){List<Row> old=rows,next=new ArrayList<>();for(RpgCatalog.Entry e:entries)next.add(new Row(e));DiffUtil.DiffResult changes=DiffUtil.calculateDiff(new DiffUtil.Callback(){public int getOldListSize(){return Math.max(1,old.size())+1;}public int getNewListSize(){return Math.max(1,next.size())+1;}public boolean areItemsTheSame(int a,int b){if(a==0||b==0)return a==b;if(old.isEmpty()||next.isEmpty())return old.isEmpty()&&next.isEmpty();return old.get(a-1).entry.id.equals(next.get(b-1).entry.id);}public boolean areContentsTheSame(int a,int b){return a==0||!old.isEmpty()&&!next.isEmpty()&&old.get(a-1).stamp.equals(next.get(b-1).stamp);}});rows=next;changes.dispatchUpdatesTo(this);}
    }
}
