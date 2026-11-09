package com.nealaringale.nextbelladmin;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class AdminActivity extends Activity {
    private final int navy = Color.rgb(16, 30, 53);
    private final int blue = Color.rgb(57, 105, 220);
    private final int ink = Color.rgb(28, 39, 56);
    private final int muted = Color.rgb(104, 117, 138);
    private final int pale = Color.rgb(243, 246, 251);
    private EditText titleInput, bodyInput;
    private Spinner audienceInput, priorityInput, typeInput;
    private LinearLayout draftsList;
    private JSONArray drafts;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        drafts = loadDrafts();
        render();
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(pale);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(22), dp(20), dp(32));
        scroll.addView(page);

        TextView brand = text("NEXTBELL  /  CONTROL ROOM", 12, blue, true);
        page.addView(brand);
        TextView heading = text("Admin console", 30, navy, true);
        LinearLayout.LayoutParams hp = params(-1, -2); hp.topMargin = dp(8);
        page.addView(heading, hp);
        TextView sub = text("Create announcements for the right students.", 15, muted, false);
        LinearLayout.LayoutParams sp = params(-1, -2); sp.topMargin = dp(5); sp.bottomMargin = dp(18);
        page.addView(sub, sp);

        LinearLayout status = card();
        TextView statusTitle = text("PUBLISHING STATUS", 11, blue, true);
        status.addView(statusTitle);
        TextView statusBody = text("Setup mode · Cloud delivery not connected", 15, ink, true);
        LinearLayout.LayoutParams stp = params(-1, -2); stp.topMargin = dp(8);
        status.addView(statusBody, stp);
        TextView statusNote = text("Drafts save on this device. Push notifications need the secure NextBell backend before Publish can go live.", 13, muted, false);
        LinearLayout.LayoutParams snp = params(-1, -2); snp.topMargin = dp(6);
        status.addView(statusNote, snp);
        page.addView(status, params(-1, -2));

        section(page, "NEW ANNOUNCEMENT");
        titleInput = edit("Announcement title");
        page.addView(titleInput, gapParams());
        bodyInput = edit("Write the announcement and include any important time, room or deadline.");
        bodyInput.setMinLines(4);
        bodyInput.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        page.addView(bodyInput, gapParams());

        typeInput = spinner(new String[]{"General update", "Timetable change", "Assignment / deadline", "Exam notice", "Holiday / cancellation"});
        audienceInput = spinner(new String[]{"Everyone", "FY B.Tech · All divisions", "Division B", "Batch B1 · Rolls 1–25", "Batch B2 · Rolls 26–50", "Batch B3 · Rolls 51+"});
        priorityInput = spinner(new String[]{"Normal priority", "Important", "Urgent"});
        field(page, "NOTICE TYPE", typeInput);
        field(page, "SEND TO", audienceInput);
        field(page, "PRIORITY", priorityInput);

        Button save = button("Save draft", navy);
        LinearLayout.LayoutParams bp = params(-1, dp(52)); bp.topMargin = dp(10);
        page.addView(save, bp);
        save.setOnClickListener(v -> saveDraft());
        Button publish = button("Publish announcement  →", blue);
        LinearLayout.LayoutParams pp = params(-1, dp(52)); pp.topMargin = dp(10);
        page.addView(publish, pp);
        publish.setOnClickListener(v -> new AlertDialog.Builder(this)
            .setTitle("Cloud delivery not configured")
            .setMessage("The admin screen is ready for the next integration step, but publishing now would not reach student phones. We need to connect a secured Firebase project and the student app first. Your draft can be saved safely on this device.")
            .setPositiveButton("Save as draft", (d, w) -> saveDraft())
            .setNegativeButton("Cancel", null).show());

        section(page, "SAVED DRAFTS");
        TextView draftNote = text("Local drafts · visible only on this admin device", 13, muted, false);
        page.addView(draftNote);
        draftsList = new LinearLayout(this);
        draftsList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams dl = params(-1, -2); dl.topMargin = dp(8);
        page.addView(draftsList, dl);
        renderDrafts();
        setContentView(scroll);
    }

    private void saveDraft() {
        String title = titleInput.getText().toString().trim();
        String body = bodyInput.getText().toString().trim();
        if (title.isEmpty() || body.isEmpty()) {
            Toast.makeText(this, "Add a title and announcement first.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONObject item = new JSONObject();
            item.put("title", title);
            item.put("body", body);
            item.put("type", typeInput.getSelectedItem().toString());
            item.put("audience", audienceInput.getSelectedItem().toString());
            item.put("priority", priorityInput.getSelectedItem().toString());
            item.put("created", System.currentTimeMillis());
            drafts.put(0, item);
            getSharedPreferences("admin_local", MODE_PRIVATE).edit().putString("drafts", drafts.toString()).apply();
            renderDrafts();
            Toast.makeText(this, "Draft saved on this device.", Toast.LENGTH_SHORT).show();
            titleInput.setText(""); bodyInput.setText("");
        } catch (Exception e) {
            Toast.makeText(this, "Couldn't save this draft.", Toast.LENGTH_SHORT).show();
        }
    }

    private JSONArray loadDrafts() {
        try { return new JSONArray(getSharedPreferences("admin_local", MODE_PRIVATE).getString("drafts", "[]")); }
        catch (Exception e) { return new JSONArray(); }
    }

    private void renderDrafts() {
        if (draftsList == null) return;
        draftsList.removeAllViews();
        if (drafts.length() == 0) {
            TextView empty = text("No drafts yet. Create an announcement above.", 14, muted, false);
            LinearLayout.LayoutParams ep = params(-1, -2); ep.topMargin = dp(8);
            draftsList.addView(empty, ep); return;
        }
        for (int i = 0; i < drafts.length(); i++) {
            JSONObject item = drafts.optJSONObject(i);
            if (item == null) continue;
            LinearLayout row = card();
            TextView t = text(item.optString("title", "Untitled"), 16, ink, true);
            row.addView(t);
            TextView meta = text(item.optString("audience", "Everyone") + "  ·  " + item.optString("priority", "Normal priority"), 12, blue, true);
            LinearLayout.LayoutParams mp = params(-1, -2); mp.topMargin = dp(6);
            row.addView(meta, mp);
            TextView b = text(item.optString("body", ""), 13, muted, false);
            LinearLayout.LayoutParams bpar = params(-1, -2); bpar.topMargin = dp(6);
            row.addView(b, bpar);
            LinearLayout.LayoutParams rp = params(-1, -2); rp.topMargin = dp(8);
            draftsList.addView(row, rp);
        }
    }

    private void field(LinearLayout parent, String label, View view) {
        TextView l = text(label, 11, muted, true);
        LinearLayout.LayoutParams lp = params(-1, -2); lp.topMargin = dp(10); lp.bottomMargin = dp(5);
        parent.addView(l, lp);
        parent.addView(view, params(-1, dp(50)));
    }
    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        s.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values));
        s.setPadding(dp(10), 0, dp(10), 0);
        s.setBackground(shape(Color.WHITE, Color.rgb(220, 227, 238), 12));
        return s;
    }
    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(false); e.setTextSize(15); e.setTextColor(ink); e.setHintTextColor(muted);
        e.setHint(hint); e.setPadding(dp(14), dp(12), dp(14), dp(12));
        e.setBackground(shape(Color.WHITE, Color.rgb(220, 227, 238), 12));
        return e;
    }
    private void section(LinearLayout p, String value) {
        TextView t = text(value, 11, muted, true);
        LinearLayout.LayoutParams lp = params(-1, -2); lp.topMargin = dp(24); lp.bottomMargin = dp(10);
        p.addView(t, lp);
    }
    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15), dp(14), dp(15), dp(14));
        c.setBackground(shape(Color.WHITE, Color.rgb(229, 234, 242), 18));
        return c;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private Button button(String label, int color) {
        Button b = new Button(this); b.setText(label); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setTextSize(15); b.setBackground(shape(color, color, 14)); return b;
    }
    private GradientDrawable shape(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke); return d;
    }
    private LinearLayout.LayoutParams gapParams() {
        LinearLayout.LayoutParams p = params(-1, -2); p.bottomMargin = dp(10); return p;
    }
    private LinearLayout.LayoutParams params(int w, int h) { return new LinearLayout.LayoutParams(w, h); }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
}
