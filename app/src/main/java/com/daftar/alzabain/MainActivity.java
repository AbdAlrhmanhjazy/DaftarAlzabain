package com.daftar.alzabain;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftar_prefs";
    private DatabaseHelper db;
    private SharedPreferences prefs;

    private LinearLayout tabHome, tabCustomers, tabReports, tabSettings;
    private LinearLayout navHome, navCustomers, navReports, navSettings;
    private TextView navHomeIcon, navHomeText, navCustomersIcon, navCustomersText;
    private TextView navReportsIcon, navReportsText, navSettingsIcon, navSettingsText;
    private TextView fabAddCustomer;
    private ScrollView svMainContent;

    private TextView tvShopOwner, tvTotalRemaining, tvDebtorsCount, tvWhatYouOwe;
    private TextView tvTodayDebts, tvTodayPayments;
    private TextView tvSlot2Symbol, tvSlot2Name, tvSlot3Symbol, tvSlot3Name, tvSlot4Symbol, tvSlot4Name;
    private LinearLayout llRecentCustomers, llCustomersList, llRecentTransactions;
    private EditText etSearchCustomers;
    private TextView btnSortCustomers, tvCustomersTotalBadge, tvReportDebts, tvReportPayments, tvReportNet;

    private boolean sortByBalanceDesc = false;
    private boolean hideAmounts = false;
    private final ArrayList<Integer> quickSlotCustomerIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        hideAmounts = prefs.getBoolean("hide_amounts", false);

        bindViews();
        setupNavigation();
        setupHomeActions();
        setupCustomersActions();
        setupSettingsActions();

        switchTab(0);
        refreshAll();
    }

    private void bindViews() {
        svMainContent = findViewById(R.id.svMainContent);
        tabHome = findViewById(R.id.tabHome);
        tabCustomers = findViewById(R.id.tabCustomers);
        tabReports = findViewById(R.id.tabReports);
        tabSettings = findViewById(R.id.tabSettings);

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

        tvShopOwner = findViewById(R.id.tvShopOwner);
        tvTotalRemaining = findViewById(R.id.tvTotalRemaining);
        tvDebtorsCount = findViewById(R.id.tvDebtorsCount);
        tvWhatYouOwe = findViewById(R.id.tvWhatYouOwe);
        tvTodayDebts = findViewById(R.id.tvTodayDebts);
        tvTodayPayments = findViewById(R.id.tvTodayPayments);

        tvSlot2Symbol = findViewById(R.id.tvSlot2Symbol);
        tvSlot2Name = findViewById(R.id.tvSlot2Name);
        tvSlot3Symbol = findViewById(R.id.tvSlot3Symbol);
        tvSlot3Name = findViewById(R.id.tvSlot3Name);
        tvSlot4Symbol = findViewById(R.id.tvSlot4Symbol);
        tvSlot4Name = findViewById(R.id.tvSlot4Name);

        llRecentCustomers = findViewById(R.id.llRecentCustomers);
        llCustomersList = findViewById(R.id.llCustomersList);
        llRecentTransactions = findViewById(R.id.llRecentTransactions);

        etSearchCustomers = findViewById(R.id.etSearchCustomers);
        btnSortCustomers = findViewById(R.id.btnSortCustomers);
        tvCustomersTotalBadge = findViewById(R.id.tvCustomersTotalBadge);

        tvReportDebts = findViewById(R.id.tvReportDebts);
        tvReportPayments = findViewById(R.id.tvReportPayments);
        tvReportNet = findViewById(R.id.tvReportNet);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> switchTab(0));
        navCustomers.setOnClickListener(v -> switchTab(1));
        navReports.setOnClickListener(v -> switchTab(2));
        navSettings.setOnClickListener(v -> switchTab(3));
        fabAddCustomer.setOnClickListener(v -> showAddCustomerDialog());
    }

    private void switchTab(int index) {
        tabHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabCustomers.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabReports.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabSettings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        fabAddCustomer.setVisibility(index == 1 ? View.VISIBLE : View.GONE);

        int activeColor = Color.parseColor("#14B896");
        int inactiveColor = Color.parseColor("#9FB0AA");

        navHomeIcon.setTextColor(index == 0 ? activeColor : inactiveColor);
        navHomeText.setTextColor(index == 0 ? activeColor : inactiveColor);

        navCustomersIcon.setTextColor(index == 1 ? activeColor : inactiveColor);
        navCustomersText.setTextColor(index == 1 ? activeColor : inactiveColor);

        navReportsIcon.setTextColor(index == 2 ? activeColor : inactiveColor);
        navReportsText.setTextColor(index == 2 ? activeColor : inactiveColor);

        navSettingsIcon.setTextColor(index == 3 ? activeColor : inactiveColor);
        navSettingsText.setTextColor(index == 3 ? activeColor : inactiveColor);

        svMainContent.scrollTo(0, 0);
        refreshAll();
    }

    private void setupHomeActions() {
        findViewById(R.id.tvEditShopBtn).setOnClickListener(v -> showEditShopOwnerDialog());
        findViewById(R.id.cardWalletsBanks).setOnClickListener(v -> showWalletInfoDialog());
        findViewById(R.id.btnQuickEdit).setOnClickListener(v -> showQuickSlotsPickerDialog());
        findViewById(R.id.slotAdd).setOnClickListener(v -> showAddCustomerDialog());

        findViewById(R.id.slot2).setOnClickListener(v -> handleQuickSlotClick(0));
        findViewById(R.id.slot3).setOnClickListener(v -> handleQuickSlotClick(1));
        findViewById(R.id.slot4).setOnClickListener(v -> handleQuickSlotClick(2));
    }

    private void setupCustomersActions() {
        etSearchCustomers.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                refreshCustomersList(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btnClearSearch).setOnClickListener(v -> {
            etSearchCustomers.setText("");
            refreshCustomersList("");
        });

        btnSortCustomers.setOnClickListener(v -> {
            sortByBalanceDesc = !sortByBalanceDesc;
            btnSortCustomers.setText(sortByBalanceDesc ? "ترتيب: الرصيد ▾" : "ترتيب: الاسم ▾");
            refreshCustomersList(etSearchCustomers.getText().toString());
        });
    }

    private void setupSettingsActions() {
        findViewById(R.id.itemBackup).setOnClickListener(v ->
                Toast.makeText(this, "قاعدة البيانات محفوظة تلقائياً على الجهاز (daftar_alzabain.db)", Toast.LENGTH_SHORT).show());
        findViewById(R.id.itemReports).setOnClickListener(v -> switchTab(2));
        findViewById(R.id.itemHideAmounts).setOnClickListener(v -> {
            hideAmounts = !hideAmounts;
            prefs.edit().putBoolean("hide_amounts", hideAmounts).apply();
            Toast.makeText(this, hideAmounts ? "تم إخفاء المبالغ" : "تم إظهار المبالغ", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
        findViewById(R.id.itemTemplates).setOnClickListener(v ->
                Toast.makeText(this, "قالب التذكير: مرحباً، نود تذكيركم برصيد الحساب المستحق.", Toast.LENGTH_SHORT).show());
        findViewById(R.id.itemShop).setOnClickListener(v -> showEditShopOwnerDialog());
        findViewById(R.id.itemAppearance).setOnClickListener(v ->
                Toast.makeText(this, "المظهر الداكن الزمردي مفعّل حالياً", Toast.LENGTH_SHORT).show());
        findViewById(R.id.itemSecurity).setOnClickListener(v ->
                Toast.makeText(this, "إعدادات الأمان متاحة عبر قفل الجهاز", Toast.LENGTH_SHORT).show());
        findViewById(R.id.itemAbout).setOnClickListener(v ->
                Toast.makeText(this, "دفتر الزبائن - إصدار 1.0", Toast.LENGTH_SHORT).show());
    }

    private String formatAmount(double value) {
        if (hideAmounts) {
            return "•••• ₪";
        }
        return String.format(Locale.getDefault(), "%.1f ₪", value);
    }

    private void refreshAll() {
        String ownerName = prefs.getString("shop_owner", "الحاج");
        tvShopOwner.setText(ownerName);

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        double totalDebts = 0;
        double totalPayments = 0;
        double todayDebts = 0;
        double todayPayments = 0;

        Cursor cAll = db.getReadableDatabase().rawQuery(
                "SELECT " +
                        "COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), " +
                        "COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) " +
                        "FROM transactions",
                null
        );
        if (cAll != null) {
            if (cAll.moveToFirst()) {
                totalDebts = cAll.getDouble(0);
                totalPayments = cAll.getDouble(1);
            }
            cAll.close();
        }

        Cursor cToday = db.getReadableDatabase().rawQuery(
                "SELECT " +
                        "COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), " +
                        "COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) " +
                        "FROM transactions WHERE created_at LIKE ?",
                new String[]{todayStr + "%"}
        );
        if (cToday != null) {
            if (cToday.moveToFirst()) {
                todayDebts = cToday.getDouble(0);
                todayPayments = cToday.getDouble(1);
            }
            cToday.close();
        }

        double netRemaining = totalDebts - totalPayments;
        double owedToOthers = netRemaining < 0 ? Math.abs(netRemaining) : 0;
        double positiveRemaining = Math.max(0, netRemaining);

        int debtorsCount = 0;
        Cursor cDebtors = db.getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM (" +
                        "SELECT c.id, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal " +
                        "FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id " +
                        "GROUP BY c.id HAVING bal > 0)",
                null
        );
        if (cDebtors != null) {
            if (cDebtors.moveToFirst()) {
                debtorsCount = cDebtors.getInt(0);
            }
            cDebtors.close();
        }

        tvTotalRemaining.setText(formatAmount(positiveRemaining));
        tvDebtorsCount.setText(debtorsCount + " زبوناً عليهم ديون");
        tvWhatYouOwe.setText("ما عليك: " + formatAmount(owedToOthers));
        tvTodayDebts.setText(formatAmount(todayDebts));
        tvTodayPayments.setText(formatAmount(todayPayments));

        tvReportDebts.setText("إجمالي الديون: " + formatAmount(totalDebts));
        tvReportPayments.setText("إجمالي الدفعات: " + formatAmount(totalPayments));
        tvReportNet.setText("الصافي المتبقي لك: " + formatAmount(netRemaining));

        refreshQuickSlots();
        refreshCustomersList(etSearchCustomers.getText().toString());
        refreshRecentHomeAndTransactions();
    }

    private void refreshQuickSlots() {
        quickSlotCustomerIds.clear();
        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT id, name FROM customers ORDER BY id ASC LIMIT 3",
                null
        );
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        if (c != null) {
            while (c.moveToNext()) {
                ids.add(c.getInt(0));
                names.add(c.getString(1));
            }
            c.close();
        }

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
        if (slotIndex < quickSlotCustomerIds.size() && quickSlotCustomerIds.get(slotIndex) != -1) {
            int custId = quickSlotCustomerIds.get(slotIndex);
            String name = getCustomerNameById(custId);
            showAddTransactionDialog(custId, name);
        } else {
            showAddCustomerDialog();
        }
    }

    private void refreshCustomersList(String query) {
        llCustomersList.removeAllViews();

        String sql = "SELECT c.id, c.name, c.phone, " +
                "COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance " +
                "FROM customers c " +
                "LEFT JOIN transactions t ON c.id = t.customer_id ";
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            sql += "WHERE c.name LIKE ? OR c.phone LIKE ? ";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }
        sql += "GROUP BY c.id, c.name, c.phone ";
        sql += sortByBalanceDesc ? "ORDER BY balance DESC, c.name ASC" : "ORDER BY c.name ASC";

        Cursor cursor = db.getReadableDatabase().rawQuery(sql, args);
        int count = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count++;
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                String phone = cursor.getString(2);
                double balance = cursor.getDouble(3);

                LinearLayout row = buildCustomerCardRow(id, name, phone, balance);
                llCustomersList.addView(row);
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
        bg.setColor(Color.parseColor("#1B2421"));
        bg.setCornerRadius(36f);
        bg.setStroke(2, Color.parseColor("#293531"));
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 14);
        card.setLayoutParams(cardParams);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        leftCol.setGravity(Gravity.START);

        TextView tvBal = new TextView(this);
        tvBal.setText(formatAmount(Math.abs(balance)));
        tvBal.setTextColor(balance >= 0 ? Color.parseColor("#FF6B6B") : Color.parseColor("#2DD4A8"));
        tvBal.setTextSize(16f);

        TextView tvStatus = new TextView(this);
        tvStatus.setText(balance > 0 ? "عليه" : (balance < 0 ? "له" : "متزن"));
        tvStatus.setTextColor(Color.parseColor("#9FB0AA"));
        tvStatus.setTextSize(12f);

        leftCol.addView(tvBal);
        leftCol.addView(tvStatus);

        LinearLayout midCol = new LinearLayout(this);
        midCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams midParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        midParams.setMargins(16, 0, 16, 0);
        midCol.setLayoutParams(midParams);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16f);
        midCol.addView(tvName);

        if (phone != null && !phone.trim().isEmpty()) {
            TextView tvPhone = new TextView(this);
            tvPhone.setText(phone);
            tvPhone.setTextColor(Color.parseColor("#9FB0AA"));
            tvPhone.setTextSize(12f);
            midCol.addView(tvPhone);
        }

        TextView tvAvatar = new TextView(this);
        String initial = (name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1) : "ز";
        tvAvatar.setText(initial);
        tvAvatar.setTextColor(Color.WHITE);
        tvAvatar.setTextSize(15f);
        tvAvatar.setGravity(Gravity.CENTER);

        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(Color.parseColor("#25332E"));
        tvAvatar.setBackground(avatarBg);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(84, 84);
        tvAvatar.setLayoutParams(avatarParams);

        card.addView(leftCol);
        card.addView(midCol);
        card.addView(tvAvatar);

        card.setOnClickListener(v -> showAddTransactionDialog(id, name));
        return card;
    }

    private void refreshRecentHomeAndTransactions() {
        llRecentCustomers.removeAllViews();
        llRecentTransactions.removeAllViews();

        Cursor c = db.getReadableDatabase().rawQuery(
                "SELECT c.id, c.name, c.phone, " +
                        "COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance " +
                        "FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id " +
                        "GROUP BY c.id, c.name, c.phone ORDER BY c.id DESC LIMIT 3",
                null
        );
        if (c != null) {
            while (c.moveToNext()) {
                llRecentCustomers.addView(buildCustomerCardRow(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getDouble(3)
                ));
            }
            c.close();
        }

        Cursor ct = db.getReadableDatabase().rawQuery(
                "SELECT t.type, t.amount, t.note, t.created_at, c.name " +
                        "FROM transactions t JOIN customers c ON t.customer_id = c.id " +
                        "ORDER BY t.id DESC LIMIT 10",
                null
        );
        if (ct != null) {
            while (ct.moveToNext()) {
                String type = ct.getString(0);
                double amount = ct.getDouble(1);
                String note = ct.getString(2);
                String dateStr = ct.getString(3);
                String custName = ct.getString(4);

                TextView item = new TextView(this);
                String tag = "DEBT".equals(type) ? "دين" : "دفعة";
                String notePart = (note != null && !note.trim().isEmpty()) ? " - " + note : "";
                String datePart = (dateStr != null && !dateStr.trim().isEmpty()) ? " (" + dateStr + ")" : "";
                item.setText(custName + " | " + tag + ": " + formatAmount(amount) + notePart + datePart);
                item.setTextColor(Color.WHITE);
                item.setTextSize(14f);
                item.setPadding(24, 24, 24, 24);

                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#1B2421"));
                bg.setCornerRadius(28f);
                item.setBackground(bg);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 0, 0, 12);
                item.setLayoutParams(params);

                llRecentTransactions.addView(item);
            }
            ct.close();
        }
    }

    private String getCustomerNameById(int id) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT name FROM customers WHERE id = ?", new String[]{String.valueOf(id)});
        String name = "زبون";
        if (c != null) {
            if (c.moveToFirst()) {
                name = c.getString(0);
            }
            c.close();
        }
        return name;
    }

    private void showEditShopOwnerDialog() {
        final EditText etOwner = new EditText(this);
        etOwner.setText(prefs.getString("shop_owner", "الحاج"));
        etOwner.setHint("الاسم أو اسم المحل");

        new AlertDialog.Builder(this)
                .setTitle("تعديل الاسم الظاهر")
                .setView(etOwner)
                .setPositiveButton("حفظ", (d, w) -> {
                    String val = etOwner.getText().toString().trim();
                    if (!val.isEmpty()) {
                        prefs.edit().putString("shop_owner", val).apply();
                        refreshAll();
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showWalletInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("المحافظ والبنوك")
                .setMessage("جوال باي • بال باي • بنك فلسطين")
                .setPositiveButton("حسناً", null)
                .show();
    }

    private void showQuickSlotsPickerDialog() {
        Toast.makeText(this, "يتم عرض أول 3 زبائن مضافين في التسجيل السريع، اضغط على أي بطاقة لتسجيل دين أو دفعة.", Toast.LENGTH_SHORT).show();
    }

    private void showAddCustomerDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etName = new EditText(this);
        etName.setHint("اسم الزبون");
        layout.addView(etName);

        final EditText etPhone = new EditText(this);
        etPhone.setHint("رقم الجوال (اختياري)");
        etPhone.setInputType(InputType.TYPE_CLASS_PHONE);
        layout.addView(etPhone);

        new AlertDialog.Builder(this)
                .setTitle("إضافة زبون")
                .setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String phone = etPhone.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "يرجى إدخال اسم الزبون", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("phone", phone);
                    db.getWritableDatabase().insert("customers", null, values);
                    refreshAll();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showAddTransactionDialog(int customerId, String customerLabel) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        final EditText etAmount = new EditText(this);
        etAmount.setHint("المبلغ");
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etAmount);

        final EditText etNote = new EditText(this);
        etNote.setHint("ملاحظة (اختياري)");
        layout.addView(etNote);

        new AlertDialog.Builder(this)
                .setTitle(customerLabel)
                .setView(layout)
                .setPositiveButton("تسجيل دين", (dialog, which) -> saveTransaction(customerId, "DEBT", etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNeutralButton("تسجيل دفعة", (dialog, which) -> saveTransaction(customerId, "PAYMENT", etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void saveTransaction(int customerId, String type, String amountStr, String note) {
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show();
                return;
            }
            String nowStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put("customer_id", customerId);
            values.put("type", type);
            values.put("amount", amount);
            values.put("note", note);
            values.put("created_at", nowStr);
            db.getWritableDatabase().insert("transactions", null, values);
            refreshAll();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "صيغة المبلغ غير صحيحة", Toast.LENGTH_SHORT).show();
        }
    }
}