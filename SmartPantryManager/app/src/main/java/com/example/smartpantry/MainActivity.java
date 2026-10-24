package com.example.smartpantry;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.view.*;
import android.widget.*;
import java.util.*;

/*
 * Smart Pantry Manager
 * Demonstration implementation: SQLite-backed pantry CRUD and strict recipe matching.
 */
public class MainActivity extends Activity {
    PantryDb db;
    LinearLayout root, content;
    int green = 0xFF2E6D55;
    int selectedRecipeId = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        db = new PantryDb(this);
        showHome();
    }

    TextView heading(String text) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(23); t.setTextColor(green);
        t.setPadding(0, 12, 0, 12);
        return t;
    }

    void shell(String title) {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 18, 24, 18);
        root.setBackgroundColor(0xFFF8FAF7);
        TextView h = heading(title); root.addView(h);
        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        LinearLayout nav = new LinearLayout(this);
        addNav(nav, "Pantry", this::showHome);
        addNav(nav, "Add item", this::addItem);
        addNav(nav, "Recipes", this::showRecipes);
        addNav(nav, "Settings", this::showSettings);
        navScroll.addView(nav); root.addView(navScroll);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    void addNav(LinearLayout nav, String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setOnClickListener(v -> action.run());
        nav.addView(b);
    }
    Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setOnClickListener(v -> action.run());
        content.addView(b); return b;
    }
    EditText field(String hint) {
        EditText e = new EditText(this); e.setHint(hint); content.addView(e); return e;
    }
    void message(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void showHome() {
        shell("Smart Pantry Manager");
        content.addView(heading("Your pantry"));
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id,name,quantity,unit FROM pantry ORDER BY name", null);
        if (!c.moveToFirst()) content.addView(new TextView(this) {{ setText("Your pantry is empty. Add ingredients to get started."); setTextSize(16); }});
        else {
            do {
                final int id = c.getInt(0); final String name = c.getString(1);
                TextView row = new TextView(this);
                row.setText(name + " — " + c.getDouble(2) + " " + c.getString(3));
                row.setTextSize(17); row.setPadding(0, 10, 0, 10); content.addView(row);
                LinearLayout actions = new LinearLayout(this);
                Button edit = new Button(this); edit.setText("Edit");
                edit.setOnClickListener(v -> editItem(id, name));
                Button delete = new Button(this); delete.setText("Delete");
                delete.setOnClickListener(v -> new AlertDialog.Builder(this).setMessage("Delete " + name + "?")
                    .setPositiveButton("Delete", (d,w) -> { db.getWritableDatabase().delete("pantry","id=?",new String[]{String.valueOf(id)}); showHome(); })
                    .setNegativeButton("Cancel", null).show());
                actions.addView(edit); actions.addView(delete); content.addView(actions);
            } while (c.moveToNext());
        }
        c.close();
        button("Find recipes I can make", this::showRecipes);
    }

    void addItem() {
        shell("Add pantry ingredient");
        EditText name = field("Ingredient name (e.g. tomato)");
        EditText qty = field("Quantity (number)");
        EditText unit = field("Unit (e.g. pieces, g, ml)");
        button("Save ingredient", () -> {
            String n = name.getText().toString().trim();
            String q = qty.getText().toString().trim();
            String u = unit.getText().toString().trim();
            if (n.isEmpty() || q.isEmpty() || u.isEmpty()) { message("Please complete all fields."); return; }
            try {
                double quantity = Double.parseDouble(q);
                if (quantity <= 0) { message("Quantity must be greater than zero."); return; }
                android.content.ContentValues cv = new android.content.ContentValues();
                cv.put("name", normalize(n)); cv.put("quantity", quantity); cv.put("unit", u);
                db.getWritableDatabase().insert("pantry", null, cv);
                message("Ingredient saved."); showHome();
            } catch (NumberFormatException ex) { message("Enter a valid number for quantity."); }
        });
    }

    void editItem(int id, String oldName) {
        shell("Edit ingredient");
        EditText name = field("Ingredient name"); name.setText(oldName);
        EditText qty = field("New quantity");
        EditText unit = field("Unit");
        Cursor c = db.getReadableDatabase().rawQuery("SELECT quantity,unit FROM pantry WHERE id=?",new String[]{String.valueOf(id)});
        if(c.moveToFirst()){ qty.setText(c.getString(0)); unit.setText(c.getString(1)); } c.close();
        button("Update ingredient", () -> {
            String n=name.getText().toString().trim(), q=qty.getText().toString().trim(), u=unit.getText().toString().trim();
            if(n.isEmpty()||q.isEmpty()||u.isEmpty()){message("Please complete all fields.");return;}
            try {
                double amount=Double.parseDouble(q); if(amount<=0){message("Quantity must be positive.");return;}
                android.content.ContentValues cv=new android.content.ContentValues();
                cv.put("name",normalize(n)); cv.put("quantity",amount); cv.put("unit",u);
                db.getWritableDatabase().update("pantry",cv,"id=?",new String[]{String.valueOf(id)});
                showHome();
            } catch(Exception e){message("Enter a valid quantity.");}
        });
    }

    String normalize(String s) {
        String x=s.toLowerCase(Locale.ROOT).trim();
        if(x.endsWith("ies") && x.length()>4) x=x.substring(0,x.length()-3)+"y";
        else if(x.endsWith("oes") && x.length()>4) x=x.substring(0,x.length()-2);
        else if(x.endsWith("s") && !x.endsWith("ss") && x.length()>3) x=x.substring(0,x.length()-1);
        return x;
    }

    static class Recipe {
        int id; String name, ingredients, steps;
        Recipe(int i,String n,String ing,String s){id=i;name=n;ingredients=ing;steps=s;}
    }

    List<Recipe> recipes() {
        List<Recipe> list=new ArrayList<>();
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,name,ingredients,steps FROM recipes ORDER BY name",null);
        while(c.moveToNext()) list.add(new Recipe(c.getInt(0),c.getString(1),c.getString(2),c.getString(3)));
        c.close(); return list;
    }

    Set<String> pantryNames() {
        Set<String> names=new HashSet<>();
        Cursor c=db.getReadableDatabase().rawQuery("SELECT name FROM pantry",null);
        while(c.moveToNext()) names.add(normalize(c.getString(0)));
        c.close(); return names;
    }

    void showRecipes() {
        shell("Suggested recipes");
        Set<String> available=pantryNames(); int count=0;
        content.addView(heading("Only recipes with every required ingredient"));
        for(Recipe r:recipes()) {
            String[] required=r.ingredients.split(",");
            boolean match=true;
            for(String ingredient:required) if(!available.contains(normalize(ingredient))) { match=false; break; }
            if(match) {
                count++; TextView item= new TextView(this);
                item.setText("• "+r.name); item.setTextSize(18); item.setPadding(0,12,0,12);
                item.setOnClickListener(v -> showRecipeDetail(r)); content.addView(item);
                Button details=new Button(this); details.setText("View recipe"); details.setOnClickListener(v -> showRecipeDetail(r)); content.addView(details);
            }
        }
        if(count==0) content.addView(new TextView(this) {{ setText("No recipes match your pantry yet — add more ingredients."); setTextSize(16); }});
    }

    void showRecipeDetail(Recipe r) {
        shell("Recipe details");
        content.addView(heading(r.name));
        content.addView(new TextView(this) {{ setText("Required ingredients: "+r.ingredients.replace(",", ", ")); setTextSize(17); }});
        content.addView(new TextView(this) {{ setText("Preparation method:\n"+r.steps.replace("|","\n")); setTextSize(16); setPadding(0,16,0,16); }});
        button("Back to suggestions",this::showRecipes);
    }

    void showSettings() {
        shell("Settings");
        content.addView(heading("Preferences"));
        content.addView(new TextView(this) {{ setText("Smart Pantry Manager stores pantry items and seeded recipes locally using SQLite. No location or mapping services are used."); setTextSize(16); }});
        Switch alerts = new Switch(this); alerts.setText("Expiry reminders (demo preference)");
        alerts.setChecked(false); content.addView(alerts);
        button("Save preference", () -> message("Preference applied for this session."));
        button("Back to pantry", this::showHome);
    }

    static class PantryDb extends SQLiteOpenHelper {
        PantryDb(Context c){super(c,"smart_pantry.db",null,1);}
        public void onCreate(SQLiteDatabase d) {
            d.execSQL("CREATE TABLE pantry(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,quantity REAL NOT NULL,unit TEXT NOT NULL)");
            d.execSQL("CREATE TABLE recipes(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,ingredients TEXT NOT NULL,steps TEXT NOT NULL)");
            seed(d);
        }
        void seed(SQLiteDatabase d) {
            String[][] data={
                {"Tomato Egg Scramble","egg,tomato,salt","Beat eggs with salt|Chop tomato and cook in a pan|Add eggs and stir until cooked"},
                {"Garlic Tomato Pasta","pasta,tomato,garlic,salt","Boil pasta|Cook tomato and garlic|Mix together and season"},
                {"Banana Oat Bowl","banana,oat,milk","Slice banana|Combine oats and milk|Top with banana"},
                {"Cheese Toast","bread,cheese","Place cheese on bread|Toast until melted"},
                {"Simple Pancakes","flour,egg,milk","Mix flour, egg and milk|Pour into a warm pan|Cook both sides"},
                {"Potato Hash","potato,onion,salt","Dice potato and onion|Cook until tender|Season with salt"},
                {"Apple Oats","apple,oat,cinnamon","Chop apple|Cook with oats and cinnamon|Serve warm"},
                {"Tuna Sandwich","bread,tuna,mayonnaise","Mix tuna and mayonnaise|Spread on bread|Serve"},
                {"Peanut Banana Toast","bread,peanut butter,banana","Toast bread|Spread peanut butter|Add sliced banana"},
                {"Rice and Peas","rice,peas,salt","Cook rice|Heat peas|Combine and season"},
                {"Lemon Water","lemon,water","Slice lemon|Add to water|Serve chilled"},
                {"Yogurt Fruit Bowl","yogurt,banana,apple","Chop fruit|Add yogurt|Mix gently"},
                {"Garlic Rice","rice,garlic,oil","Cook rice|Fry garlic lightly in oil|Mix with rice"},
                {"Spinach Omelette","egg,spinach,salt","Beat eggs|Cook spinach briefly|Add eggs and cook through"},
                {"Carrot Salad","carrot,lemon,salt","Grate carrot|Add lemon juice|Season lightly"},
                {"Corn Fritters","corn,flour,egg","Mix corn, flour and egg|Spoon into a pan|Cook until firm"},
                {"Chickpea Salad","chickpea,tomato,cucumber","Chop vegetables|Mix with chickpeas|Serve"},
                {"Avocado Toast","bread,avocado,salt","Toast bread|Mash avocado|Spread and season"},
                {"Milk Porridge","oat,milk,sugar","Cook oats in milk|Stir until soft|Add sugar if desired"},
                {"Bean Rice Bowl","rice,bean,tomato","Cook rice|Heat beans with tomato|Serve together"}
            };
            for(String[] r:data) {
                android.content.ContentValues v=new android.content.ContentValues();
                v.put("name",r[0]); v.put("ingredients",r[1]); v.put("steps",r[2]);
                d.insert("recipes",null,v);
            }
        }
        public void onUpgrade(SQLiteDatabase d,int oldV,int newV) { }
    }
}
