package com.daftar.alzabain;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftar_prefs";
    private DatabaseHelper db;
    private SharedPreferences prefs;

    private LinearLayout layoutActivation;
    private TextView tvDeviceIdCode;
    private EditText etActivationCode;

    private LinearLayout tabHome, tabCustomers, tabReports, tabSettings, tabCustomerDetail;
    private LinearLayout navHome, navCustomers, navReports, navSettings;
    private TextView navHomeIcon, navHomeText, navCustomersIcon, navCustomersText;
    private TextView navReportsIcon, navReportsText, navSettingsIcon, navSettingsText;
    private TextView fabAddCustomer;
    private ScrollView svMainContent;

    private TextView tvGreeting, tvShopOwner, tvTotalRemaining, tvDebtorsCount, tvWhatYouOwe;
    private TextView tvTodayDebts, tvTodayPayments, tvCurrencySub;
    private TextView tvSlot2Symbol, tvSlot2Name, tvSlot3Symbol, tvSlot3Name, tvSlot4Symbol, tvSlot4Name;
    private LinearLayout llRecentCustomers, llCustomersList, llRecentTransactions, llDetailHistory;
    private EditText etSearchCustomers;
    private TextView btnSortCustomers, tvCustomersTotalBadge, tvReportDebts, tvReportPayments, tvReportNet;
    private TextView tvDetailName, tvDetailPhone, tvDetailBalance, tvDetailStatus;

    private boolean sortByBalanceDesc = false;
    private boolean hideAmounts = false;
    private int currentTab = 0;
    private int selectedCustomerId = -1;
    private String selectedCustomerName = "";
    private String selectedCustomerPhone = "";
    private double selectedCustomerBalance = 0;
    private final ArrayList<Integer> quickSlotCustomerIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hideAmounts = prefs.getBoolean("hide_amounts", false);

        bindViews();
        setupActivationGate();
        setupNavigation();
        setupHomeActions();
        setupCustomersActions();
        setupReportsActions();
        setupSettingsActions();
        setupDetailActions();

        switchTab(0);
        refreshAll();
    }

    private long getDeviceNumericId() {
        long saved = prefs.getLong("device_num_id", 0);
        if (saved >= 100000 && saved <= 999999) return saved;

        String aid = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (aid == null || aid.isEmpty()) aid = "DAFTAR_USER_2026";
        long h = 0;
        for (int i = 0; i < aid.length(); i++) {
            h = (h * 31 + aid.charAt(i)) % 900000;
        }
        long devNum = 100000 + Math.abs(h);
        prefs.edit().putLong("device_num_id", devNum).apply();
        return devNum;
    }

    private long computeActivationCode(long deviceNum) {
        return ((deviceNum * 7919L) + 202697L) % 900000L + 100000L;
    }

    private void setupActivationGate() {
        if (prefs.getBoolean("is_activated", false)) {
            layoutActivation.setVisibility(View.GONE);
            return;
        }
        layoutActivation.setVisibility(View.VISIBLE);
        long devNum = getDeviceNumericId();
        String devStr = String.valueOf(devNum);
        tvDeviceIdCode.setText(devStr);

        findViewById(R.id.btnCopyDeviceId).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("device_id", devStr));
                Toast.makeText(this, "تم نسخ رقم الجهاز: " + devStr, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnActivateApp).setOnClickListener(v -> {
            String entered = etActivationCode.getText().toString().trim();
            String expected = String.valueOf(computeActivationCode(devNum));
            if (entered.equals(expected)) {
                prefs.edit().putBoolean("is_activated", true).apply();
                layoutActivation.setVisibility(View.GONE);
                Toast.makeText(this, "تم تفعيل التطبيق بنجاح! مرحباً بك", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "كود التفعيل غير صحيح، تأكد من الرقم", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindViews() {
        layoutActivation = findViewById(R.id.layoutActivation);
        tvDeviceIdCode = findViewById(R.id.tvDeviceIdCode);
        etActivationCode = findViewById(R.id.etActivationCode);

        svMainContent = findViewById(R.id.svMainContent);
        tabHome = findViewById(R.id.tabHome);
        tabCustomers = findViewById(R.id.tabCustomers);
        tabReports = findViewById(R.id.tabReports);
        tabSettings = findViewById(R.id.tabSettings);
        tabCustomerDetail = findViewById(R.id.tabCustomerDetail);

        navHome = findViewById(R.id.navHome);
        navCustomers = findViewById(R.id.navCustomers);
        navReports = findViewById(R.id.navReports);
        navSettings = findViewById(R.id.navSettings);

        navHomeIcon = findViewById(R.id.navHomeIcon);
        navHomeText = findViewById(R.id.navHomeText);
        navCustomersIcon = findViewById(R.id.navCustomersIcon);
        navCustomersText = findViewById(R.id.navCustomersText);
        navReportsIcon = findViewById(R.id.navReportsIcon);
        navReportsText = findViewById(R.id.navReportsText);
        navSettingsIcon = findViewById(R.id.navSettingsIcon);
        navSettingsText = findViewById(R.id.navSettingsText);

        fabAddCustomer = findViewById(R.id.fabAddCustomer);

        tvGreeting = findViewById(R.id.tvGreeting);
        tvShopOwner = findViewById(R.id.tvShopOwner);
        tvTotalRemaining = findViewById(R.id.tvTotalRemaining);
        tvDebtorsCount = findViewById(R.id.tvDebtorsCount);
        tvWhatYouOwe = findViewById(R.id.tvWhatYouOwe);
        tvTodayDebts = findViewById(R.id.tvTodayDebts);
        tvTodayPayments = findViewById(R.id.tvTodayPayments);
        tvCurrencySub = findViewById(R.id.tvCurrencySub);

        tvSlot2Symbol = findViewById(R.id.tvSlot2Symbol);
        tvSlot2Name = findViewById(R.id.tvSlot2Name);
        tvSlot3Symbol = findViewById(R.id.tvSlot3Symbol);
        tvSlot3Name = findViewById(R.id.tvSlot3Name);
        tvSlot4Symbol = findViewById(R.id.tvSlot4Symbol);
        tvSlot4Name = findViewById(R.id.tvSlot4Name);

        llRecentCustomers = findViewById(R.id.llRecentCustomers);
        llCustomersList = findViewById(R.id.llCustomersList);
        llRecentTransactions = findViewById(R.id.llRecentTransactions);
        llDetailHistory = findViewById(R.id.llDetailHistory);

        etSearchCustomers = findViewById(R.id.etSearchCustomers);
        btnSortCustomers = findViewById(R.id.btnSortCustomers);
        tvCustomersTotalBadge = findViewById(R.id.tvCustomersTotalBadge);

        tvReportDebts = findViewById(R.id.tvReportDebts);
        tvReportPayments = findViewById(R.id.tvReportPayments);
        tvReportNet = findViewById(R.id.tvReportNet);

        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPhone = findViewById(R.id.tvDetailPhone);
        tvDetailBalance = findViewById(R.id.tvDetailBalance);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> switchTab(0));
        navCustomers.setOnClickListener(v -> switchTab(1));
        navReports.setOnClickListener(v -> switchTab(2));
        navSettings.setOnClickListener(v -> switchTab(3));
        fabAddCustomer.setOnClickListener(v -> showAddCustomerDialog());
    }

    private void switchTab(int index) {
        currentTab = index;
        tabHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabCustomers.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabReports.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabSettings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        tabCustomerDetail.setVisibility(index == 4 ? View.VISIBLE : View.GONE);
        fabAddCustomer.setVisibility(index == 1 ? View.VISIBLE : View.GONE);

        int activeColor = Color.parseColor("#14B896");
        int inactiveColor = Color.parseColor("#96ACA4");

        navHomeIcon.setTextColor(index == 0 ? activeColor : inactiveColor);
        navHomeText.setTextColor(index == 0 ? activeColor : inactiveColor);
        navCustomersIcon.setTextColor((index == 1 || index == 4) ? activeColor : inactiveColor);
        navCustomersText.setTextColor((index == 1 || index == 4) ? activeColor : inactiveColor);
        navReportsIcon.setTextColor(index == 2 ? activeColor : inactiveColor);
        navReportsText.setTextColor(index == 2 ? activeColor : inactiveColor);
        navSettingsIcon.setTextColor(index == 3 ? activeColor : inactiveColor);
        navSettingsText.setTextColor(index == 3 ? activeColor : inactiveColor);

        svMainContent.scrollTo(0, 0);
        refreshAll();
    }

    @Override
    public void onBackPressed() {
        if (currentTab == 4) {
            switchTab(1);
        } else {
            super.onBackPressed();
        }
    }

    private void setupHomeActions() {
        findViewById(R.id.tvEditShopBtn).setOnClickListener(v -> showEditShopOwnerDialog());
        findViewById(R.id.cardWalletsBanks).setOnClickListener(v -> showWalletsDialog());
        findViewById(R.id.btnQuickEdit).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.slotAdd).setOnClickListener(v -> showAddCustomerDialog());
        findViewById(R.id.slot2).setOnClickListener(v -> handleQuickSlotClick(0));
        findViewById(R.id.slot3).setOnClickListener(v -> handleQuickSlotClick(1));
        findViewById(R.id.slot4).setOnClickListener(v -> handleQuickSlotClick(2));
    }

    private void setupCustomersActions() {
        etSearchCustomers.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshCustomersList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.btnClearSearch).setOnClickListener(v -> { etSearchCustomers.setText(""); refreshCustomersList(""); });
        btnSortCustomers.setOnClickListener(v -> {
            sortByBalanceDesc = !sortByBalanceDesc;
            btnSortCustomers.setText(sortByBalanceDesc ? "ترتيب: الرصيد ▾" : "ترتيب: الاسم ▾");
            refreshCustomersList(etSearchCustomers.getText().toString());
        });
    }

    private void setupReportsActions() {
        findViewById(R.id.btnShareFullReport).setOnClickListener(v -> shareFullReport());
    }

    private void setupSettingsActions() {
        findViewById(R.id.itemShop).setOnClickListener(v -> showEditShopOwnerDialog());
        findViewById(R.id.itemCurrency).setOnClickListener(v -> cycleCurrency());
        findViewById(R.id.itemHideAmounts).setOnClickListener(v -> {
            hideAmounts = !hideAmounts;
            prefs.edit().putBoolean("hide_amounts", hideAmounts).apply();
            Toast.makeText(this, hideAmounts ? "تم إخفاء المبالغ" : "تم إظهار المبالغ", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
        findViewById(R.id.itemBackup).setOnClickListener(v -> shareFullReport());
        findViewById(R.id.itemAbout).setOnClickListener(v ->
                Toast.makeText(this, "دفتر الزبائن الاحترافي 3.0 - رقم الجهاز: " + getDeviceNumericId(), Toast.LENGTH_LONG).show());
    }

    private void setupDetailActions() {
        findViewById(R.id.btnBackFromDetail).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.btnEditCustomerDetail).setOnClickListener(v -> showEditOrDeleteCustomerDialog());
        findViewById(R.id.btnDetailAddDebt).setOnClickListener(v -> showSingleTypeTransactionDialog(selectedCustomerId, selectedCustomerName, "DEBT"));
        findViewById(R.id.btnDetailAddPayment).setOnClickListener(v -> showSingleTypeTransactionDialog(selectedCustomerId, selectedCustomerName, "PAYMENT"));
        findViewById(R.id.btnDetailWhatsApp).setOnClickListener(v -> openWhatsAppReminder());
        findViewById(R.id.btnDetailCall).setOnClickListener(v -> callSelectedCustomer());
        findViewById(R.id.btnDetailShare).setOnClickListener(v -> shareCustomerStatement());
    }

    private String getCurrency() { return prefs.getString("currency", "₪"); }

    private void cycleCurrency() {
        String cur = getCurrency();
        String next = cur.equals("₪") ? "$" : (cur.equals("$") ? "د.أ" : "₪");
        prefs.edit().putString("currency", next).apply();
        Toast.makeText(this, "تم تغيير العملة إلى: " + next, Toast.LENGTH_SHORT).show();
        refreshAll();
    }

    private String formatAmount(double value) {
        if (hideAmounts) return "•••• " + getCurrency();
        return String.format(Locale.getDefault(), "%.1f %s", value, getCurrency());
    }

    private AlertDialog.Builder darkDialog() { return new AlertDialog.Builder(this, R.style.DarkDialogTheme); }

    private EditText styledEditText(String hint, String value, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value);
        et.setInputType(inputType);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#96ACA4"));
        et.setPadding(24, 24, 24, 24);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#111A17"));
        bg.setCornerRadius(20f);
        bg.setStroke(2, Color.parseColor("#263530"));
        et.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 18);
        et.setLayoutParams(lp);
        return et;
    }

    private void refreshAll() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(hour < 12 ? "صباح الخير" : "مساء الخير");
        tvShopOwner.setText(prefs.getString("shop_owner", "الحاج"));
        tvCurrencySub.setText("الحالية: " + getCurrency() + " - اضغط للتغيير بين ₪ و $ و د.أ");

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        double totalDebts = 0, totalPayments = 0, todayDebts = 0, todayPayments = 0;

        Cursor cAll = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) FROM transactions", null);
        if (cAll != null) { if (cAll.moveToFirst()) { totalDebts = cAll.getDouble(0); totalPayments = cAll.getDouble(1); } cAll.close(); }

        Cursor cToday = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) FROM transactions WHERE created_at LIKE ?", new String[]{todayStr + "%"});
        if (cToday != null) { if (cToday.moveToFirst()) { todayDebts = cToday.getDouble(0); todayPayments = cToday.getDouble(1); } cToday.close(); }

        double netRemaining = totalDebts - totalPayments;
        int debtorsCount = 0;
        Cursor cDebtors = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM (SELECT c.id, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id HAVING bal > 0)", null);
        if (cDebtors != null) { if (cDebtors.moveToFirst()) debtorsCount = cDebtors.getInt(0); cDebtors.close(); }

        tvTotalRemaining.setText(formatAmount(Math.max(0, netRemaining)));
        tvDebtorsCount.setText(debtorsCount + " زبوناً عليهم ديون");
        tvWhatYouOwe.setText("ما عليك: " + formatAmount(netRemaining < 0 ? Math.abs(netRemaining) : 0));
        tvTodayDebts.setText(formatAmount(todayDebts));
        tvTodayPayments.setText(formatAmount(todayPayments));

        tvReportDebts.setText("إجمالي الديون: " + formatAmount(totalDebts));
        tvReportPayments.setText("إجمالي الدفعات: " + formatAmount(totalPayments));
        tvReportNet.setText("الصافي المتبقي لك: " + formatAmount(netRemaining));

        refreshQuickSlots();
        refreshCustomersList(etSearchCustomers.getText().toString());
        refreshRecentHomeAndTransactions();
        if (currentTab == 4 && selectedCustomerId != -1) loadCustomerDetail(selectedCustomerId);
    }

    private void openCustomerDetail(int customerId) { selectedCustomerId = customerId; switchTab(4); }

    private void loadCustomerDetail(int customerId) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.name, c.phone, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id WHERE c.id = ? GROUP BY c.id", new String[]{String.valueOf(customerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                selectedCustomerName = c.getString(0);
                selectedCustomerPhone = c.getString(1) != null ? c.getString(1) : "";
                selectedCustomerBalance = c.getDouble(2);
            }
            c.close();
        }
        tvDetailName.setText(selectedCustomerName);
        tvDetailPhone.setText(selectedCustomerPhone.isEmpty() ? "لا يوجد رقم جوال مسجل" : selectedCustomerPhone);
        tvDetailBalance.setText(formatAmount(Math.abs(selectedCustomerBalance)));
        tvDetailStatus.setText(selectedCustomerBalance > 0 ? "عليه" : (selectedCustomerBalance < 0 ? "له" : "حساب متزن"));

        llDetailHistory.removeAllViews();
        Cursor h = db.getReadableDatabase().rawQuery("SELECT id, type, amount, note, created_at FROM transactions WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(customerId)});
        int count = 0;
        if (h != null) {
            while (h.moveToNext()) {
                count++;
                int txId = h.getInt(0);
                boolean isDebt = "DEBT".equals(h.getString(1));
                double amount = h.getDouble(2);
                String note = h.getString(3);
                String dateStr = h.getString(4);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(24, 22, 24, 22);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#18221F"));
                bg.setCornerRadius(32f);
                bg.setStroke(2, Color.parseColor("#263530"));
                row.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                row.setLayoutParams(lp);

                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

                TextView tvTitle = new TextView(this);
                tvTitle.setText(isDebt ? "🔴 سحب / دين" : "🟢 دفعة مستلمة");
                tvTitle.setTextColor(Color.WHITE);
                tvTitle.setTextSize(15f);
                infoCol.addView(tvTitle);

                if (note != null && !note.trim().isEmpty()) {
                    TextView tvNote = new TextView(this);
                    tvNote.setText("ملاحظة: " + note);
                    tvNote.setTextColor(Color.parseColor("#C7D8D2"));
                    tvNote.setTextSize(13f);
                    infoCol.addView(tvNote);
                }

                TextView tvDate = new TextView(this);
                tvDate.setText(dateStr != null ? dateStr : "");
                tvDate.setTextColor(Color.parseColor("#96ACA4"));
                tvDate.setTextSize(12f);
                infoCol.addView(tvDate);

                TextView tvAmt = new TextView(this);
                tvAmt.setText((isDebt ? "+ " : "- ") + formatAmount(amount));
                tvAmt.setTextColor(isDebt ? Color.parseColor("#FF6B6B") : Color.parseColor("#2DD4A8"));
                tvAmt.setTextSize(17f);

                row.addView(infoCol);
                row.addView(tvAmt);
                row.setOnClickListener(v -> confirmDeleteTransaction(txId));
                llDetailHistory.addView(row);
            }
            h.close();
        }
        if (count == 0) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد حركات مسجلة لهذا الزبون حتى الآن.");
            empty.setTextColor(Color.parseColor("#96ACA4"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(20, 40, 20, 40);
            llDetailHistory.addView(empty);
        }
    }

    private void confirmDeleteTransaction(int txId) {
        darkDialog().setTitle("حذف الحركة").setMessage("هل تريد حذف هذه الحركة من سجل الزبون؟")
                .setPositiveButton("حذف", (d, w) -> { db.getWritableDatabase().delete("transactions", "id = ?", new String[]{String.valueOf(txId)}); refreshAll(); })
                .setNegativeButton("إلغاء", null).show();
    }

    private void openWhatsAppReminder() {
        String shop = prefs.getString("shop_owner", "المحل");
        String status = selectedCustomerBalance > 0 ? "عليكم رصيد مستحق بقيمة" : "رصيدكم الحالي هو";
        String msg = "مرحباً " + selectedCustomerName + "، معك " + shop + ". نود تذكيركم بأن " + status + " " + formatAmount(Math.abs(selectedCustomerBalance)) + ". شكراً لتعاملكم معنا.";
        try {
            String phone = selectedCustomerPhone.replaceAll("[^0-9]", "");
            String url = phone.isEmpty() ? "https://wa.me/?text=" + URLEncoder.encode(msg, "UTF-8") : "https://wa.me/" + phone + "?text=" + URLEncoder.encode(msg, "UTF-8");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) { Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show(); }
    }

    private void callSelectedCustomer() {
        if (selectedCustomerPhone.trim().isEmpty()) { Toast.makeText(this, "لا يوجد رقم جوال مسجل", Toast.LENGTH_SHORT).show(); return; }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + selectedCustomerPhone.trim())));
    }

    private void shareCustomerStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 كشف حساب الزبون: ").append(selectedCustomerName).append("\n");
        sb.append("المحل: ").append(prefs.getString("shop_owner", "الحاج")).append("\n");
        sb.append("الرصيد الحالي: ").append(formatAmount(Math.abs(selectedCustomerBalance))).append(selectedCustomerBalance > 0 ? " (عليه)" : (selectedCustomerBalance < 0 ? " (له)" : " (متزن)")).append("\n--------------------\n");
        Cursor h = db.getReadableDatabase().rawQuery("SELECT type, amount, note, created_at FROM transactions WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(selectedCustomerId)});
        if (h != null) {
            while (h.moveToNext()) {
                sb.append("• ").append("DEBT".equals(h.getString(0)) ? "دين" : "دفعة").append(": ").append(formatAmount(h.getDouble(1))).append(" | ").append(h.getString(3));
                String note = h.getString(2);
                if (note != null && !note.trim().isEmpty()) sb.append(" (").append(note).append(")");
                sb.append("\n");
            }
            h.close();
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "مشاركة كشف الحساب"));
    }

    private void shareFullReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 تقرير دفتر الزبائن - ").append(prefs.getString("shop_owner", "الحاج")).append("\n");
        sb.append(tvReportDebts.getText()).append("\n").append(tvReportPayments.getText()).append("\n").append(tvReportNet.getText()).append("\n--------------------\n");
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.name, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id ORDER BY bal DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                double bal = c.getDouble(1);
                sb.append("• ").append(c.getString(0)).append(": ").append(formatAmount(Math.abs(bal))).append(bal > 0 ? " (عليه)" : (bal < 0 ? " (له)" : " (متزن)")).append("\n");
            }
            c.close();
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "مشاركة التقرير الكامل"));
    }

    private void showEditOrDeleteCustomerDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 10);
        final EditText etName = styledEditText("اسم الزبون", selectedCustomerName, InputType.TYPE_CLASS_TEXT);
        final EditText etPhone = styledEditText("رقم الجوال", selectedCustomerPhone, InputType.TYPE_CLASS_PHONE);
        layout.addView(etName); layout.addView(etPhone);
        darkDialog().setTitle("تعديل بيانات الزبون").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = etName.getText().toString().trim();
                    if (!n.isEmpty()) {
                        ContentValues cv = new ContentValues(); cv.put("name", n); cv.put("phone", etPhone.getText().toString().trim());
                        db.getWritableDatabase().update("customers", cv, "id = ?", new String[]{String.valueOf(selectedCustomerId)});
                        refreshAll();
                    }
                })
                .setNeutralButton("حذف الزبون", (d, w) -> {
                    db.getWritableDatabase().delete("transactions", "customer_id = ?", new String[]{String.valueOf(selectedCustomerId)});
                    db.getWritableDatabase().delete("customers", "id = ?", new String[]{String.valueOf(selectedCustomerId)});
                    switchTab(1);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void showWalletsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 10);
        final EditText etJawwal = styledEditText("رقم محفظة جوال باي", prefs.getString("w_jawwal", ""), InputType.TYPE_CLASS_PHONE);
        final EditText etPalpay = styledEditText("رقم محفظة بال باي", prefs.getString("w_palpay", ""), InputType.TYPE_CLASS_PHONE);
        final EditText etBank = styledEditText("رقم حساب بنك فلسطين", prefs.getString("w_bank", ""), InputType.TYPE_CLASS_TEXT);
        layout.addView(etJawwal); layout.addView(etPalpay); layout.addView(etBank);
        darkDialog().setTitle("المحافظ والبنوك").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    prefs.edit().putString("w_jawwal", etJawwal.getText().toString().trim()).putString("w_palpay", etPalpay.getText().toString().trim()).putString("w_bank", etBank.getText().toString().trim()).apply();
                    Toast.makeText(this, "تم حفظ بيانات المحافظ", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("نسخ الكل", (d, w) -> {
                    String txt = "جوال باي: " + etJawwal.getText().toString().trim() + "\nبال باي: " + etPalpay.getText().toString().trim() + "\nبنك فلسطين: " + etBank.getText().toString().trim();
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) { cm.setPrimaryClip(ClipData.newPlainText("wallets", txt)); Toast.makeText(this, "تم نسخ أرقام المحافظ", Toast.LENGTH_SHORT).show(); }
                })
                .setNegativeButton("إغلاق", null).show();
    }

    private void refreshQuickSlots() {
        quickSlotCustomerIds.clear();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id, name FROM customers ORDER BY id DESC LIMIT 3", null);
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) { ids.add(c.getInt(0)); names.add(c.getString(1)); } c.close(); }
        String[] defaultSymbols = {"ر", "ي", "ع"};
        TextView[] symbols = {tvSlot2Symbol, tvSlot3Symbol, tvSlot4Symbol};
        TextView[] labels = {tvSlot2Name, tvSlot3Name, tvSlot4Name};
        for (int i = 0; i < 3; i++) {
            if (i < ids.size()) {
                quickSlotCustomerIds.add(ids.get(i));
                String n = names.get(i).trim();
                symbols[i].setText(n.isEmpty() ? defaultSymbols[i] : n.substring(0, 1));
                labels[i].setText(n);
            } else {
                quickSlotCustomerIds.add(-1);
                symbols[i].setText(defaultSymbols[i]);
                labels[i].setText("زبون سريع");
            }
        }
    }

    private void handleQuickSlotClick(int slotIndex) {
        if (slotIndex < quickSlotCustomerIds.size() && quickSlotCustomerIds.get(slotIndex) != -1) openCustomerDetail(quickSlotCustomerIds.get(slotIndex));
        else showAddCustomerDialog();
    }

    private void refreshCustomersList(String query) {
        llCustomersList.removeAllViews();
        String sql = "SELECT c.id, c.name, c.phone, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id ";
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            sql += "WHERE c.name LIKE ? OR c.phone LIKE ? ";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }
        sql += "GROUP BY c.id, c.name, c.phone " + (sortByBalanceDesc ? "ORDER BY balance DESC, c.name ASC" : "ORDER BY c.name ASC");
        Cursor cursor = db.getReadableDatabase().rawQuery(sql, args);
        int count = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count++;
                llCustomersList.addView(buildCustomerCardRow(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getDouble(3)));
            }
            cursor.close();
        }
        tvCustomersTotalBadge.setText(count + " زبون");
    }

    private LinearLayout buildCustomerCardRow(int id, String name, String phone, double balance) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(24, 24, 24, 24);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#18221F"));
        bg.setCornerRadius(36f);
        bg.setStroke(2, Color.parseColor("#263530"));
        card.setBackground(bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cardParams);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvBal = new TextView(this);
        tvBal.setText(formatAmount(Math.abs(balance)));
        tvBal.setTextColor(balance >= 0 ? Color.parseColor("#FF6B6B") : Color.parseColor("#2DD4A8"));
        tvBal.setTextSize(16f);
        TextView tvStatus = new TextView(this);
        tvStatus.setText(balance > 0 ? "عليه" : (balance < 0 ? "له" : "متزن"));
        tvStatus.setTextColor(Color.parseColor("#96ACA4"));
        tvStatus.setTextSize(12f);
        leftCol.addView(tvBal); leftCol.addView(tvStatus);

        LinearLayout midCol = new LinearLayout(this);
        midCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams midParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        midParams.setMargins(16, 0, 16, 0);
        midCol.setLayoutParams(midParams);
        TextView tvName = new TextView(this);
        tvName.setText(name); tvName.setTextColor(Color.WHITE); tvName.setTextSize(16f);
        midCol.addView(tvName);
        if (phone != null && !phone.trim().isEmpty()) {
            TextView tvPhone = new TextView(this);
            tvPhone.setText(phone); tvPhone.setTextColor(Color.parseColor("#96ACA4")); tvPhone.setTextSize(12f);
            midCol.addView(tvPhone);
        }

        TextView tvAvatar = new TextView(this);
        tvAvatar.setText((name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1) : "ز");
        tvAvatar.setTextColor(Color.WHITE); tvAvatar.setTextSize(15f); tvAvatar.setGravity(Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.parseColor("#21342E"));
        tvAvatar.setBackground(avatarBg);
        tvAvatar.setLayoutParams(new LinearLayout.LayoutParams(84, 84));

        card.addView(leftCol); card.addView(midCol); card.addView(tvAvatar);
        card.setOnClickListener(v -> openCustomerDetail(id));
        return card;
    }

    private void refreshRecentHomeAndTransactions() {
        llRecentCustomers.removeAllViews();
        llRecentTransactions.removeAllViews();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.id, c.name, c.phone, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id, c.name, c.phone ORDER BY c.id DESC LIMIT 4", null);
        if (c != null) { while (c.moveToNext()) llRecentCustomers.addView(buildCustomerCardRow(c.getInt(0), c.getString(1), c.getString(2), c.getDouble(3))); c.close(); }

        Cursor ct = db.getReadableDatabase().rawQuery("SELECT t.type, t.amount, t.note, t.created_at, c.name, c.id FROM transactions t JOIN customers c ON t.customer_id = c.id ORDER BY t.id DESC LIMIT 15", null);
        if (ct != null) {
            while (ct.moveToNext()) {
                TextView item = new TextView(this);
                String tag = "DEBT".equals(ct.getString(0)) ? "🔴 دين" : "🟢 دفعة";
                String note = ct.getString(2);
                String notePart = (note != null && !note.trim().isEmpty()) ? " - " + note : "";
                String dateStr = ct.getString(3);
                item.setText(ct.getString(4) + " | " + tag + ": " + formatAmount(ct.getDouble(1)) + notePart + (dateStr != null ? "\n" + dateStr : ""));
                item.setTextColor(Color.WHITE); item.setTextSize(14f); item.setPadding(24, 22, 24, 22);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#18221F")); bg.setCornerRadius(28f); bg.setStroke(2, Color.parseColor("#263530"));
                item.setBackground(bg);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 0, 12);
                item.setLayoutParams(params);
                int custId = ct.getInt(5);
                item.setOnClickListener(v -> openCustomerDetail(custId));
                llRecentTransactions.addView(item);
            }
            ct.close();
        }
    }

    private void showEditShopOwnerDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etOwner = styledEditText("الاسم أو اسم المحل", prefs.getString("shop_owner", "الحاج"), InputType.TYPE_CLASS_TEXT);
        layout.addView(etOwner);
        darkDialog().setTitle("تعديل اسم المحل").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> { String val = etOwner.getText().toString().trim(); if (!val.isEmpty()) { prefs.edit().putString("shop_owner", val).apply(); refreshAll(); } })
                .setNegativeButton("إلغاء", null).show();
    }

    private void showAddCustomerDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etName = styledEditText("اسم الزبون", "", InputType.TYPE_CLASS_TEXT);
        final EditText etPhone = styledEditText("رقم الجوال (اختياري)", "", InputType.TYPE_CLASS_PHONE);
        layout.addView(etName); layout.addView(etPhone);
        darkDialog().setTitle("إضافة زبون جديد").setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "يرجى إدخال اسم الزبون", Toast.LENGTH_SHORT).show(); return; }
                    ContentValues values = new ContentValues(); values.put("name", name); values.put("phone", etPhone.getText().toString().trim());
                    long newId = db.getWritableDatabase().insert("customers", null, values);
                    refreshAll();
                    if (newId > 0) openCustomerDetail((int) newId);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void showSingleTypeTransactionDialog(int customerId, String customerLabel, String type) {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etAmount = styledEditText("المبلغ (" + getCurrency() + ")", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText etNote = styledEditText("ملاحظة (اختياري)", "", InputType.TYPE_CLASS_TEXT);
        layout.addView(etAmount); layout.addView(etNote);
        darkDialog().setTitle(("DEBT".equals(type) ? "تسجيل دين على: " : "تسجيل دفعة من: ") + customerLabel).setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> saveTransaction(customerId, type, etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNegativeButton("إلغاء", null).show();
    }

    private void saveTransaction(int customerId, String type, String amountStr, String note) {
        if (amountStr.isEmpty()) { Toast.makeText(this, "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show(); return; }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) { Toast.makeText(this, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show(); return; }
            ContentValues values = new ContentValues();
            values.put("customer_id", customerId);
            values.put("type", type);
            values.put("amount", amount);
            values.put("note", note);
            values.put("created_at", new SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.US).format(new Date()));
            db.getWritableDatabase().insert("transactions", null, values);
            refreshAll();
        } catch (NumberFormatException e) { Toast.makeText(this, "صيغة المبلغ غير صحيحة", Toast.LENGTH_SHORT).show(); }
    }
}