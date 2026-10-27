package com.mehekcar.rc;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 static final int GREEN=0xff375a29, GOLD=0xffcb9e45, CREAM=0xfff7efdc, BROWN=0xff82451e, RED=0xffb6251c;
 static final String APP_TITLE="Mehek BT Controler";
 static final UUID SPP=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 BluetoothAdapter adapter; BluetoothSocket socket; OutputStream output;
 TextView status, speedLabel; Spinner devices; SeekBar speed;
 ArrayList<BluetoothDevice> paired=new ArrayList<>();
 float scale=1f;
 int theme=2;
 final String[] themes={"Midnight Cyan","Electric Neon Blue","Tarar Bari Green & Gold","Purple & Gold","Classic Royal Blue","Sunset Amber","Magenta Night","Emerald Forest","Rose Gold","Arctic Ice"};
 int themeColor(int c){
  int[][] palettes={
   {0xff126a80,0xff58d5e6,0xff071522,0xff17405e,0xffd34257},
   {0xff0750ad,0xff40cfff,0xff091d48,0xff174eaa,0xffe63349},
   {0xff375a29,0xffcb9e45,0xfff7efdc,0xff82451e,0xffb6251c},
   {0xff5b3f89,0xffffc66d,0xfff1e9f6,0xff48345f,0xffbb243b},
   {0xff0750ad,0xff40cfff,0xffeaf2ff,0xff174eaa,0xffe63349},
   {0xffce6a1c,0xffffd16d,0xfffff2df,0xff82451e,0xffc73532},
   {0xffa93681,0xffff8fdc,0xfffaeaf6,0xff63325f,0xffec335d},
   {0xff176b45,0xffd6b35b,0xfff6f0e2,0xff276c46,0xffc42f30},
   {0xffa45d64,0xffeac0a5,0xfffff3ec,0xff864f58,0xffc73b4c},
   {0xff297a9c,0xff9de9fa,0xffeffaff,0xff1e5d80,0xffd34257}
  };
  int[] a=palettes[Math.max(0,Math.min(theme,9))];
  if(c==GREEN||c==0xff263f1d||c==0xff203e22||c==0xff375a29||c==0xff61703c)return a[0];
  if(c==GOLD||c==0xfff4d177||c==0xffb99845||c==0xffeac367||c==0xffca933a)return a[1];
  if(c==CREAM||c==0xffe6dbbe||c==0xfff6f0e2||c==0xffe4d9bd)return a[2];
  if(c==BROWN||c==0xff75451d||c==0xff5d3213)return a[3];
  if(c==RED||c==0xff8e1715)return a[4];
  return c;
 }
 void selectTheme(){new android.app.AlertDialog.Builder(this).setTitle("Choose controller design").setSingleChoiceItems(themes,theme,(dialog,which)->{
   theme=Math.max(0,Math.min(themes.length-1,which));getPreferences(0).edit().putInt("theme",theme).apply();int n=speed.getProgress();ui();speed.setProgress(n);if(adapter!=null && (Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED))loadDevices();if(output!=null)status.setText("●  Connected");dialog.dismiss();
 }).setNegativeButton("Cancel",null).show();}

 int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 @Override public void onCreate(Bundle b){super.onCreate(b);adapter=BluetoothAdapter.getDefaultAdapter();theme=Math.max(0,Math.min(themes.length-1,getPreferences(0).getInt("theme",2)));float available=getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density-90f;scale=Math.min(1f,Math.max(.60f,available/740f));ui();
   if(adapter==null){status.setText("Bluetooth unavailable");return;}
   if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},200);
   else loadDevices();
 }
 GradientDrawable bg(int a,int b,int radius,int stroke){
   GradientDrawable g=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{themeColor(a),themeColor(b)});
   g.setCornerRadius(dp(radius));if(stroke!=0)g.setStroke(dp(2),themeColor(stroke));return g;
 }
 LinearLayout col(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(0);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 LinearLayout.LayoutParams m(int w,int h,int margin){
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(w),h<0?h:dp(h*scale));
   p.setMargins(dp(margin*scale),dp(margin*scale),dp(margin*scale),dp(margin*scale));return p;
 }
 LinearLayout.LayoutParams weight(int h){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(h*scale),1);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}
 TextView label(String s,int size,int color,boolean bold){
   TextView t=new TextView(this);t.setText(s);t.setTextSize(size*scale);t.setTextColor(themeColor(color));
   t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;
 }
 Button btn(String s,int a,int b,int txt){
   Button v=new Button(this);v.setAllCaps(false);v.setText(s);v.setTextColor((txt==Color.WHITE)?Color.WHITE:themeColor(txt));
   v.setTextSize(12*scale);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
   v.setBackground(bg(a,b,12,GOLD));v.setElevation(dp(5));return v;
 }
 Button move(String name,String cmd,int color){
   Button v=btn(name,color,color==GREEN?0xff203e22:color==BROWN?0xff5d3213:0xff8e1715,Color.WHITE);
   v.setOnTouchListener((view,event)->{
     if(event.getAction()==MotionEvent.ACTION_DOWN){v.setAlpha(.75f);send(cmd);return true;}
     if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){
       v.setAlpha(1f);if(!cmd.equals("S"))send("S");view.performClick();return true;
     }return true;
   });return v;
 }
 android.graphics.drawable.Drawable artwork(final boolean header){
  return new android.graphics.drawable.Drawable(){
   final android.graphics.Paint p=new android.graphics.Paint(3);
   @Override public void draw(android.graphics.Canvas c){
    android.graphics.Rect r=getBounds();float w=r.width(),h=r.height();
    p.setShader(new android.graphics.LinearGradient(0,0,w,h,header?themeColor(0xff263f1d):themeColor(CREAM),header?themeColor(0xff61703c):themeColor(0xffe6dbbe),android.graphics.Shader.TileMode.CLAMP));
    c.drawRoundRect(0,0,w,h,dp(header?14:0),dp(header?14:0),p);p.setShader(null);
    p.setStyle(android.graphics.Paint.Style.STROKE);p.setStrokeWidth(dp(1));
    p.setColor(header?0x5596b56e:0x22a28d60);
    for(int i=0;i<9;i++){
     float x=w*(i+1)/10f;
     android.graphics.Path path=new android.graphics.Path();path.moveTo(x,0);
     path.cubicTo(x-dp(24),h*.3f,x+dp(20),h*.7f,x-dp(10),h);
     c.drawPath(path,p);
    }
    p.setStyle(android.graphics.Paint.Style.FILL);
    if(header){
     p.setColor(0x664b7b38);c.drawCircle(w*.76f,h*.64f,dp(29),p);
     p.setColor(0x557fa34a);c.drawCircle(w*.88f,h*.37f,dp(20),p);
     p.setColor(0x998a9e4b);c.drawRect(w*.65f,h*.78f,w*.97f,h*.83f,p);
     for(int i=0;i<5;i++)c.drawCircle(w*(.69f+i*.06f),h*.85f,dp(3),p);
     p.setColor(themeColor(GOLD));p.setStyle(android.graphics.Paint.Style.STROKE);
     p.setStrokeWidth(dp(1.4f));c.drawRoundRect(dp(1),dp(1),w-dp(1),h-dp(1),dp(13),dp(13),p);p.setStyle(android.graphics.Paint.Style.FILL);
    }
   }
   @Override public void setAlpha(int a){}
   @Override public void setColorFilter(android.graphics.ColorFilter f){}
   @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
  };
 }
 void ui(){
  LinearLayout root=col();root.setPadding(dp(8),dp(7),dp(8),dp(7));root.setBackground(artwork(false));
  root.setGravity(Gravity.CENTER_VERTICAL);setContentView(root);
  LinearLayout head=row();head.setPadding(dp(6),dp(5),dp(6),dp(5));head.setBackground(artwork(true));head.setElevation(dp(4));
  ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
  head.addView(logo,m(49,49,0));
  LinearLayout brand=col();brand.setPadding(dp(5),0,0,0);
  brand.addView(label("TEAM TARAR BARI",12,Color.WHITE,true));
  brand.addView(label(APP_TITLE,12,0xfff4d177,true));
  brand.addView(label("— VOYAGER OF LIGHT —",8,Color.WHITE,false));
  head.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
  Button change=btn("🎨 THEME",0xff263f1d,0xff203e22,0xfff4d177);change.setTextSize(9*scale);
  change.setOnClickListener(v->selectTheme());head.addView(change,m(77,34,0));root.addView(head,m(-1,-2,1));
  LinearLayout connectCard=col();connectCard.setPadding(dp(6),dp(3),dp(6),dp(3));
  connectCard.setBackground(bg(0xfff6f0e2,0xffe4d9bd,12,GOLD));
  LinearLayout statusRow=row();status=label("●  Disconnected",11,0xff263f1d,true);
  statusRow.addView(status,new LinearLayout.LayoutParams(0,-2,1));
  Button connect=btn("CONNECT",GREEN,0xff203e22,Color.WHITE);
  connect.setOnClickListener(v->connect());statusRow.addView(connect,m(91,30,0));connectCard.addView(statusRow);
  devices=new Spinner(this);devices.setBackground(bg(0xfff6f0e2,0xffe4d9bd,9,GOLD));
  connectCard.addView(devices,m(-1,27,0));root.addView(connectCard,m(-1,-2,3));
  LinearLayout speedCard=col();speedCard.setPadding(dp(7),dp(2),dp(7),dp(2));
  speedCard.setBackground(bg(0xfff6f0e2,0xffe4d9bd,12,GOLD));
  speedLabel=label("◉  SPEED LEVEL  8",12,0xff263f1d,true);
  speedCard.addView(speedLabel,m(-1,20,0));speed=new SeekBar(this);speed.setMax(9);speed.setProgress(8);
  speed.setProgressTintList(ColorStateList.valueOf(themeColor(GOLD)));
  speed.setThumbTintList(ColorStateList.valueOf(themeColor(GREEN)));
  speedCard.addView(speed,m(-1,24,0));
  speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar s,int p,boolean user){speedLabel.setText("◉  SPEED LEVEL  "+p);if(user)send(""+p);}
   public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
  });root.addView(speedCard,m(-1,-2,3));
  LinearLayout pad=col();pad.setPadding(dp(5),dp(4),dp(5),dp(4));
  pad.setBackground(bg(0xfff6f0e2,0xffe4d9bd,13,GOLD));
  LinearLayout up=row();up.addView(new View(this),weight(57));
  up.addView(move("▲\nFORWARD","F",GREEN),weight(57));up.addView(new View(this),weight(57));pad.addView(up);
  LinearLayout middle=row();middle.addView(move("◀\nLEFT","L",GREEN),weight(60));
  middle.addView(move("■\nSTOP","S",RED),weight(67));
  middle.addView(move("▶\nRIGHT","R",GREEN),weight(60));pad.addView(middle);
  LinearLayout down=row();down.addView(new View(this),weight(57));
  down.addView(move("▼\nBACKWARD","B",GREEN),weight(57));down.addView(new View(this),weight(57));pad.addView(down);
  root.addView(pad,m(-1,-2,4));
  LinearLayout diagonals1=row();diagonals1.addView(move("↖ FORWARD-LEFT","G",BROWN),weight(43));
  diagonals1.addView(move("↗ FORWARD-RIGHT","I",BROWN),weight(43));root.addView(diagonals1);
  LinearLayout diagonals2=row();diagonals2.addView(move("↙ BACKWARD-LEFT","H",BROWN),weight(43));
  diagonals2.addView(move("↘ BACKWARD-RIGHT","J",BROWN),weight(43));root.addView(diagonals2);
  Button max=btn("◉ MAX SPEED (Q)",0xffeac367,0xffca933a,0xff263f1d);
  max.setOnClickListener(v->{send("q");speed.setProgress(9);});root.addView(max,m(-1,37,2));
  Button disconnect=btn("⛓ DISCONNECT",0xffb6251c,0xff8e1715,Color.WHITE);
  disconnect.setOnClickListener(v->disconnect());root.addView(disconnect,m(-1,39,2));
  LinearLayout tip=row();tip.setPadding(dp(6),dp(3),dp(6),dp(3));
  tip.setBackground(bg(0xfff6f0e2,0xffe4d9bd,8,GOLD));
  TextView hint=label("ⓘ  Pair HC-05 / HC-06 in Android Bluetooth settings first.\\n     Press STOP before disconnecting.",9,0xff263f1d,false);
  tip.addView(hint);root.addView(tip,m(-1,-2,3));
  TextView footer=label("© 2026 Team TararBari · Mustafizur Rahman Rony",9,0xff75451d,false);
  footer.setGravity(Gravity.CENTER);root.addView(footer,m(-1,18,1));
 }
 void loadDevices(){
   try{
     paired.clear();ArrayList<String> names=new ArrayList<>();
     for(BluetoothDevice d:adapter.getBondedDevices()){paired.add(d);names.add((d.getName()==null?"Bluetooth device":d.getName())+" ("+d.getAddress()+")");}
     if(names.isEmpty())names.add("Pair HC-05 / HC-06 in Settings");
     devices.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,names));
   }catch(SecurityException e){status.setText("Allow Nearby Devices permission");}
 }
 void connect(){
   if(adapter==null)return;
   if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){
     requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},200);return;
   }
   int i=devices.getSelectedItemPosition();if(i<0||i>=paired.size()){status.setText("Pair HC-05 / HC-06 first");return;}
   BluetoothDevice d=paired.get(i);status.setText("●  Connecting...");
   new Thread(()->{
     BluetoothSocket s=null;
     try{s=d.createRfcommSocketToServiceRecord(SPP);adapter.cancelDiscovery();s.connect();
       BluetoothSocket connected=s;OutputStream stream=s.getOutputStream();
       runOnUiThread(()->{socket=connected;output=stream;status.setText("●  Connected: "+d.getName());});
     }catch(Exception e){try{if(s!=null)s.close();}catch(Exception ignored){}
       runOnUiThread(()->status.setText("Connection failed. Check pairing/power."));}
   }).start();
 }
 void send(String s){
   try{if(output==null){status.setText("Not connected — connect first");return;}
     output.write(s.getBytes("US-ASCII"));output.flush();
   }catch(Exception e){status.setText("Bluetooth send failed");}
 }
 void disconnect(){
   if(output!=null)send("S");
   try{if(socket!=null)socket.close();}catch(Exception ignored){}
   socket=null;output=null;status.setText("●  Disconnected");
 }
 @Override public void onRequestPermissionsResult(int code,String[] perms,int[] results){
   super.onRequestPermissionsResult(code,perms,results);
   if(code==200){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)loadDevices();else status.setText("Bluetooth permission required");}
 }
 @Override public void onDestroy(){try{if(socket!=null)socket.close();}catch(Exception ignored){}super.onDestroy();}
}