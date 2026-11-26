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
import android.os.Handler;
import android.os.Looper;
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
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "daftar_prefs";
    private DatabaseHelper db;
    private SharedPreferences prefs;

    private LinearLayout layoutActivation, layoutPinLock, layoutSplash;
    private TextView tvDeviceIdCode, btnQuickLock, tvPinSub;
    private EditText etActivationCode, etUnlockPin;

    private LinearLayout tabHome, tabCustomers, tabReports, tabSettings, tabCustomerDetail, tabQuickSelectCustomer;
    private LinearLayout navHome, navCustomers, navReports, navSettings;
    private TextView navHomeIcon, navHomeText, navCustomersIcon, navCustomersText;
    private TextView navReportsIcon, navReportsText, navSettingsIcon, navSettingsText;
    private ScrollView svMainContent;

    private TextView tvGreeting, tvShopOwner, tvTotalRemaining, tvDebtorsCount, tvWhatYouOwe;
    private TextView tvTodayDebts, tvTodayPayments, tvBiggestOwedHeader;
    private TextView tvSlot2Symbol, tvSlot2Name, tvSlot3Symbol, tvSlot3Name, tvSlot4Symbol, tvSlot4Name;
    private LinearLayout llBiggestOwed, llHomeRecentTx, llCustomersList, llDetailHistory, llQuickSelectList;
    private EditText etSearchCustomers, etSetShopName, etSetDebtCeiling;
    private TextView btnSortCustomers, tvCustomersTotalBadge, btnSetCurrency, tvLicenseStatus;
    private TextView tvRepTotalRemaining, tvRepTotalOwed, tvRepNewDebts, tvRepPayments, tvRepDebtorsCount, tvRepCreditorsCount, tvRepOverdueCount;
    private TextView repToday, repWeek, repMonth;
    private TextView custFilterAll, custFilterDebtors, custFilterCreditors, custFilterOverLimit;
    private TextView txFilterAll, txFilterDebts, txFilterPayments;
    private TextView tvDetailName, tvDetailPhone, tvDetailStatusLabel, tvDetailBalance, btnDetailAddDebt, btnDetailAddPayment, btnSettleAccount;

    private boolean sortByBalanceDesc = false;
    private boolean hideAmounts = false;
    private int reportPeriod = 2;
    private int custFilter = 0; // 0=All, 1=Debtors, 2=Creditors, 3=OverLimit
    private int homeTxFilter = 0; // 0=All, 1=DEBT, 2=PAYMENT
    private int currentTab = 0;
    private int selectedCustomerId = -1;
    private String selectedCustomerName = "";
    private String selectedCustomerPhone = "";
    private String selectedPartyType = "CUSTOMER";
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
        setupSplashAndGates();
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
        for (int i = 0; i < aid.length(); i++) { h = (h * 31 + aid.charAt(i)) % 900000; }
        long devNum = 100000 + Math.abs(h);
        prefs.edit().putLong("device_num_id", devNum).apply();
        return devNum;
    }

    private long computeActivationCode(long deviceNum) {
        return ((deviceNum * 7919L) + 202697L) % 900000L + 100000L;
    }

    private boolean isActivated() {
        return prefs.getBoolean("is_activated", false);
    }

    private void setupSplashAndGates() {
        long devNum = getDeviceNumericId();
        String devStr = String.valueOf(devNum);
        tvDeviceIdCode.setText(devStr);

        String pin = prefs.getString("app_pin", "");
        layoutPinLock.setVisibility(pin.isEmpty() ? View.GONE : View.VISIBLE);
        layoutActivation.setVisibility(isActivated() ? View.GONE : View.VISIBLE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> layoutSplash.setVisibility(View.GONE), 1100);

        findViewById(R.id.btnUnlockPin).setOnClickListener(v -> {
            String entered = etUnlockPin.getText().toString().trim();
            if (entered.equals(prefs.getString("app_pin", ""))) {
                etUnlockPin.setText("");
                layoutPinLock.setVisibility(View.GONE);
            } else {
                Toast.makeText(this, "الرمز السري غير صحيح", Toast.LENGTH_SHORT).show();
            }
        });

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
                refreshAll();
                Toast.makeText(this, "تم تفعيل النسخة الكاملة مدى الحياة!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "كود التفعيل غير صحيح", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnContinueTrial).setOnClickListener(v -> layoutActivation.setVisibility(View.GONE));
    }

    private void bindViews() {
        layoutSplash = findViewById(R.id.layoutSplash);
        layoutPinLock = findViewById(R.id.layoutPinLock);
        layoutActivation = findViewById(R.id.layoutActivation);
        tvDeviceIdCode = findViewById(R.id.tvDeviceIdCode);
        etActivationCode = findViewById(R.id.etActivationCode);
        etUnlockPin = findViewById(R.id.etUnlockPin);
        btnQuickLock = findViewById(R.id.btnQuickLock);
        tvPinSub = findViewById(R.id.tvPinSub);

        svMainContent = findViewById(R.id.svMainContent);
        tabHome = findViewById(R.id.tabHome);
        tabCustomers = findViewById(R.id.tabCustomers);
        tabReports = findViewById(R.id.tabReports);
        tabSettings = findViewById(R.id.tabSettings);
        tabCustomerDetail = findViewById(R.id.tabCustomerDetail);
        tabQuickSelectCustomer = findViewById(R.id.tabQuickSelectCustomer);

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

        tvGreeting = findViewById(R.id.tvGreeting);
        tvShopOwner = findViewById(R.id.tvShopOwner);
        tvTotalRemaining = findViewById(R.id.tvTotalRemaining);
        tvDebtorsCount = findViewById(R.id.tvDebtorsCount);
        tvWhatYouOwe = findViewById(R.id.tvWhatYouOwe);
        tvTodayDebts = findViewById(R.id.tvTodayDebts);
        tvTodayPayments = findViewById(R.id.tvTodayPayments);
        tvBiggestOwedHeader = findViewById(R.id.tvBiggestOwedHeader);

        tvSlot2Symbol = findViewById(R.id.tvSlot2Symbol);
        tvSlot2Name = findViewById(R.id.tvSlot2Name);
        tvSlot3Symbol = findViewById(R.id.tvSlot3Symbol);
        tvSlot3Name = findViewById(R.id.tvSlot3Name);
        tvSlot4Symbol = findViewById(R.id.tvSlot4Symbol);
        tvSlot4Name = findViewById(R.id.tvSlot4Name);

        llBiggestOwed = findViewById(R.id.llBiggestOwed);
        llHomeRecentTx = findViewById(R.id.llHomeRecentTx);
        llCustomersList = findViewById(R.id.llCustomersList);
        llDetailHistory = findViewById(R.id.llDetailHistory);
        llQuickSelectList = findViewById(R.id.llQuickSelectList);

        etSearchCustomers = findViewById(R.id.etSearchCustomers);
        btnSortCustomers = findViewById(R.id.btnSortCustomers);
        tvCustomersTotalBadge = findViewById(R.id.tvCustomersTotalBadge);

        custFilterAll = findViewById(R.id.custFilterAll);
        custFilterDebtors = findViewById(R.id.custFilterDebtors);
        custFilterCreditors = findViewById(R.id.custFilterCreditors);
        custFilterOverLimit = findViewById(R.id.custFilterOverLimit);

        txFilterAll = findViewById(R.id.txFilterAll);
        txFilterDebts = findViewById(R.id.txFilterDebts);
        txFilterPayments = findViewById(R.id.txFilterPayments);

        etSetShopName = findViewById(R.id.etSetShopName);
        etSetDebtCeiling = findViewById(R.id.etSetDebtCeiling);
        btnSetCurrency = findViewById(R.id.btnSetCurrency);
        tvLicenseStatus = findViewById(R.id.tvLicenseStatus);

        tvRepTotalRemaining = findViewById(R.id.tvRepTotalRemaining);
        tvRepTotalOwed = findViewById(R.id.tvRepTotalOwed);
        tvRepNewDebts = findViewById(R.id.tvRepNewDebts);
        tvRepPayments = findViewById(R.id.tvRepPayments);
        tvRepDebtorsCount = findViewById(R.id.tvRepDebtorsCount);
        tvRepCreditorsCount = findViewById(R.id.tvRepCreditorsCount);
        tvRepOverdueCount = findViewById(R.id.tvRepOverdueCount);

        repToday = findViewById(R.id.repToday);
        repWeek = findViewById(R.id.repWeek);
        repMonth = findViewById(R.id.repMonth);

        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPhone = findViewById(R.id.tvDetailPhone);
        tvDetailStatusLabel = findViewById(R.id.tvDetailStatusLabel);
        tvDetailBalance = findViewById(R.id.tvDetailBalance);
        btnDetailAddDebt = findViewById(R.id.btnDetailAddDebt);
        btnDetailAddPayment = findViewById(R.id.btnDetailAddPayment);
        btnSettleAccount = findViewById(R.id.btnSettleAccount);
    }

    private void setupNavigation() {
        navHome.setOnClickListener(v -> switchTab(0));
        navCustomers.setOnClickListener(v -> switchTab(1));
        navReports.setOnClickListener(v -> switchTab(2));
        navSettings.setOnClickListener(v -> switchTab(3));
        findViewById(R.id.fabCenterPlus).setOnClickListener(v -> switchTab(5));
        findViewById(R.id.fabCalculator).setOnClickListener(v -> showCalculatorDialog());
    }

    private void switchTab(int index) {
        currentTab = index;
        tabHome.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabCustomers.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabReports.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabSettings.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        tabCustomerDetail.setVisibility(index == 4 ? View.VISIBLE : View.GONE);
        tabQuickSelectCustomer.setVisibility(index == 5 ? View.VISIBLE : View.GONE);

        int activeColor = Color.parseColor("#19C39D");
        int inactiveColor = Color.parseColor("#93A8A1");

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
        if (currentTab == 4 || currentTab == 5) switchTab(0);
        else super.onBackPressed();
    }

    private void setupHomeActions() {
        btnQuickLock.setOnClickListener(v -> layoutPinLock.setVisibility(View.VISIBLE));
        findViewById(R.id.btnPrivacyEye).setOnClickListener(v -> {
            hideAmounts = !hideAmounts;
            prefs.edit().putBoolean("hide_amounts", hideAmounts).apply();
            refreshAll();
        });
        findViewById(R.id.cardWalletsBanks).setOnClickListener(v -> showWalletsDialog());
        findViewById(R.id.btnQuickEdit).setOnClickListener(v -> showAddPartyDialog());
        findViewById(R.id.slotAdd).setOnClickListener(v -> showAddPartyDialog());
        findViewById(R.id.slot2).setOnClickListener(v -> handleQuickSlotClick(0));
        findViewById(R.id.slot3).setOnClickListener(v -> handleQuickSlotClick(1));
        findViewById(R.id.slot4).setOnClickListener(v -> handleQuickSlotClick(2));

        txFilterAll.setOnClickListener(v -> setHomeTxFilter(0));
        txFilterDebts.setOnClickListener(v -> setHomeTxFilter(1));
        txFilterPayments.setOnClickListener(v -> setHomeTxFilter(2));

        findViewById(R.id.btnCloseQuickSelect).setOnClickListener(v -> switchTab(0));
        findViewById(R.id.btnQuickAddNewPerson).setOnClickListener(v -> showAddPartyDialog());
    }

    private void setHomeTxFilter(int f) {
        homeTxFilter = f;
        TextView[] chips = {txFilterAll, txFilterDebts, txFilterPayments};
        for (int i = 0; i < chips.length; i++) {
            if (i == f) {
                chips[i].setBackgroundResource(R.drawable.bg_chip_active);
                chips[i].setTextColor(Color.parseColor("#0E1412"));
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_dark_card);
                chips[i].setTextColor(Color.WHITE);
            }
        }
        refreshHomeLists();
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

        custFilterAll.setOnClickListener(v -> setCustFilter(0));
        custFilterDebtors.setOnClickListener(v -> setCustFilter(1));
        custFilterCreditors.setOnClickListener(v -> setCustFilter(2));
        custFilterOverLimit.setOnClickListener(v -> setCustFilter(3));
    }

    private void setCustFilter(int f) {
        custFilter = f;
        TextView[] chips = {custFilterAll, custFilterDebtors, custFilterCreditors, custFilterOverLimit};
        for (int i = 0; i < chips.length; i++) {
            if (i == f) {
                chips[i].setBackgroundResource(R.drawable.bg_chip_active);
                chips[i].setTextColor(Color.parseColor("#0E1412"));
            } else {
                chips[i].setBackgroundResource(R.drawable.bg_dark_card);
                chips[i].setTextColor(Color.WHITE);
            }
        }
        refreshCustomersList(etSearchCustomers.getText().toString());
    }

    private void setupReportsActions() {
        repToday.setOnClickListener(v -> setReportPeriod(0));
        repWeek.setOnClickListener(v -> setReportPeriod(1));
        repMonth.setOnClickListener(v -> setReportPeriod(2));
        findViewById(R.id.btnPrintPaymentsLog).setOnClickListener(v -> shareFullReport());
    }

    private void setReportPeriod(int p) {
        reportPeriod = p;
        TextView[] tabs = {repToday, repWeek, repMonth};
        for (int i = 0; i < tabs.length; i++) {
            if (i == p) {
                tabs[i].setBackgroundResource(R.drawable.bg_chip_active);
                tabs[i].setTextColor(Color.parseColor("#0E1412"));
            } else {
                tabs[i].setBackgroundResource(R.drawable.bg_dark_card);
                tabs[i].setTextColor(Color.WHITE);
            }
        }
        refreshAll();
    }

    private void setupSettingsActions() {
        btnSetCurrency.setOnClickListener(v -> cycleCurrency());
        findViewById(R.id.btnSaveShopSettings).setOnClickListener(v -> {
            String sName = etSetShopName.getText().toString().trim();
            if (!sName.isEmpty()) prefs.edit().putString("shop_owner", sName).apply();
            String ceilStr = etSetDebtCeiling.getText().toString().trim();
            double ceil = 0;
            try { if (!ceilStr.isEmpty()) ceil = Double.parseDouble(ceilStr); } catch (Exception ignored) {}
            prefs.edit().putFloat("debt_ceiling", (float) ceil).apply();
            Toast.makeText(this, "تم حفظ إعدادات المحل بنجاح", Toast.LENGTH_SHORT).show();
            refreshAll();
        });
        findViewById(R.id.btnOpenActivationFromSettings).setOnClickListener(v -> {
            if (isActivated()) {
                Toast.makeText(this, "النسخة مفعّلة بالكامل مدى الحياة ✅", Toast.LENGTH_SHORT).show();
            } else {
                layoutActivation.setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.itemAppPin).setOnClickListener(v -> showSetupPinDialog());
        findViewById(R.id.itemBackup).setOnClickListener(v -> exportBackupData());
        findViewById(R.id.itemRestoreData).setOnClickListener(v -> showRestoreDataDialog());
        findViewById(R.id.itemReminderTemplate).setOnClickListener(v -> showEditReminderTemplateDialog());
    }

    private void setupDetailActions() {
        findViewById(R.id.btnBackFromDetail).setOnClickListener(v -> switchTab(1));
        findViewById(R.id.btnEditCustomerDetail).setOnClickListener(v -> showEditOrDeleteCustomerDialog());
        btnDetailAddDebt.setOnClickListener(v -> showTransactionDialog(selectedCustomerId, selectedCustomerName, "DEBT"));
        btnDetailAddPayment.setOnClickListener(v -> showTransactionDialog(selectedCustomerId, selectedCustomerName, "PAYMENT"));
        btnSettleAccount.setOnClickListener(v -> confirmSettleAccount());
        findViewById(R.id.btnDetailCall).setOnClickListener(v -> callSelectedCustomer());
        findViewById(R.id.btnDetailWhatsApp).setOnClickListener(v -> openWhatsAppReminder());
        findViewById(R.id.btnDetailShare).setOnClickListener(v -> shareCustomerStatement());
    }

    private void showSetupPinDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etPin = styledEditText("أدخل رمزاً من 4 أرقام (اتركه فارغاً لإلغاء القفل)", prefs.getString("app_pin", ""), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(etPin);
        darkDialog().setTitle("قفل الدفتر برمز سري").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    String p = etPin.getText().toString().trim();
                    if (p.isEmpty()) {
                        prefs.edit().remove("app_pin").apply();
                        Toast.makeText(this, "تم إلغاء قفل الرمز السري", Toast.LENGTH_SHORT).show();
                    } else if (p.length() == 4) {
                        prefs.edit().putString("app_pin", p).apply();
                        Toast.makeText(this, "تم تفعيل قفل الرمز السري بنجاح", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "يجب أن يتكون الرمز من 4 أرقام", Toast.LENGTH_SHORT).show();
                    }
                    refreshAll();
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void exportBackupData() {
        StringBuilder sb = new StringBuilder("DAFTAR_BACKUP_V1\n");
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.name, c.phone, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id", null);
        if (c != null) {
            while (c.moveToNext()) {
                sb.append(c.getString(0)).append("|")
                  .append(c.getString(1) != null ? c.getString(1) : "").append("|")
                  .append(c.getString(2) != null ? c.getString(2) : "CUSTOMER").append("|")
                  .append(c.getDouble(3)).append("\n");
            }
            c.close();
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "حفظ / مشاركة النسخة الاحتياطية"));
    }

    private void showRestoreDataDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etData = styledEditText("الصق نص النسخة الاحتياطية هنا...", "", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etData);
        darkDialog().setTitle("استيراد بيانات من نسخة احتياطية").setView(layout)
                .setPositiveButton("استيراد", (d, w) -> {
                    String raw = etData.getText().toString().trim();
                    String[] lines = raw.split("\n");
                    int added = 0;
                    String now = new SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.US).format(new Date());
                    for (String line : lines) {
                        if (line.startsWith("DAFTAR_BACKUP") || !line.contains("|")) continue;
                        String[] parts = line.split("\\|");
                        if (parts.length >= 4) {
                            String name = parts[0].trim();
                            String phone = parts[1].trim();
                            String pType = parts[2].trim();
                            double bal = 0;
                            try { bal = Double.parseDouble(parts[3].trim()); } catch (Exception ignored) {}
                            if (!name.isEmpty()) {
                                ContentValues cv = new ContentValues();
                                cv.put("name", name); cv.put("phone", phone); cv.put("party_type", pType);
                                long cid = db.getWritableDatabase().insert("customers", null, cv);
                                if (cid > 0 && bal != 0) {
                                    ContentValues tv = new ContentValues();
                                    tv.put("customer_id", cid);
                                    tv.put("type", bal > 0 ? "DEBT" : "PAYMENT");
                                    tv.put("amount", Math.abs(bal));
                                    tv.put("note", "رصيد مستورد");
                                    tv.put("created_at", now);
                                    db.getWritableDatabase().insert("transactions", null, tv);
                                }
                                added++;
                            }
                        }
                    }
                    Toast.makeText(this, "تم استيراد " + added + " حساب بنجاح", Toast.LENGTH_LONG).show();
                    refreshAll();
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private String getCurrency() { return prefs.getString("currency", "₪"); }

    private void cycleCurrency() {
        String cur = getCurrency();
        String next = cur.equals("₪") ? "$" : (cur.equals("$") ? "د.أ" : "₪");
        prefs.edit().putString("currency", next).apply();
        refreshAll();
    }

    private String formatAmount(double value) {
        if (hideAmounts) return "••••";
        if (value == Math.floor(value)) return String.format(Locale.US, "%.0f %s", value, getCurrency());
        return String.format(Locale.US, "%.1f %s", value, getCurrency());
    }

    private AlertDialog.Builder darkDialog() { return new AlertDialog.Builder(this, R.style.DarkDialogTheme); }

    private EditText styledEditText(String hint, String value, int inputType) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value);
        et.setInputType(inputType);
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#93A8A1"));
        et.setPadding(24, 24, 24, 24);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#0E1412"));
        bg.setCornerRadius(20f);
        bg.setStroke(2, Color.parseColor("#25332E"));
        et.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 14);
        et.setLayoutParams(lp);
        return et;
    }

    private long getDaysSince(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return 0;
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr.substring(0, 10));
            if (d != null) {
                long diff = System.currentTimeMillis() - d.getTime();
                return Math.max(0, TimeUnit.MILLISECONDS.toDays(diff));
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private void refreshAll() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        tvGreeting.setText(hour < 12 ? "صباح الخير" : "مساء الخير");
        String shopName = prefs.getString("shop_owner", "خضروات وفواكه");
        tvShopOwner.setText(shopName);
        etSetShopName.setText(shopName);
        btnSetCurrency.setText(getCurrency().equals("₪") ? "شيكل (₪) ▾" : (getCurrency().equals("$") ? "دولار ($) ▾" : "دينار (د.أ) ▾"));

        String pin = prefs.getString("app_pin", "");
        btnQuickLock.setVisibility(pin.isEmpty() ? View.GONE : View.VISIBLE);
        tvPinSub.setText(pin.isEmpty() ? "غير مفعّل - اضغط لتعيين رمز من 4 أرقام لحماية دفترك" : "مفعّل (••••) - اضغط للتغيير أو الإلغاء");

        float ceil = prefs.getFloat("debt_ceiling", 0f);
        if (ceil > 0) etSetDebtCeiling.setText(String.valueOf((int) ceil));

        int totalParties = 0;
        Cursor cp = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers", null);
        if (cp != null) { if (cp.moveToFirst()) totalParties = cp.getInt(0); cp.close(); }
        tvLicenseStatus.setText(isActivated() ? "النسخة الكاملة مفعّلة مدى الحياة ✅ (" + totalParties + " طرف)" : "النسخة المجانية — " + totalParties + " من 5 أطراف");

        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        double todayDebts = 0, todayPayments = 0;
        Cursor cToday = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type = 'DEBT' THEN amount ELSE 0 END), 0), COALESCE(SUM(CASE WHEN type = 'PAYMENT' THEN amount ELSE 0 END), 0) FROM transactions WHERE created_at LIKE ?", new String[]{todayStr + "%"});
        if (cToday != null) { if (cToday.moveToFirst()) { todayDebts = cToday.getDouble(0); todayPayments = cToday.getDouble(1); } cToday.close(); }
        tvTodayDebts.setText(formatAmount(todayDebts));
        tvTodayPayments.setText(formatAmount(todayPayments));

        double totalForYou = 0, totalOnYou = 0;
        int debtorsCount = 0, creditorsCount = 0, overdueCount = 0;

        Cursor cAll = db.getReadableDatabase().rawQuery(
                "SELECT c.id, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal, MAX(t.created_at) AS last_tx " +
                        "FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id", null);
        if (cAll != null) {
            while (cAll.moveToNext()) {
                String pType = cAll.getString(1);
                double bal = cAll.getDouble(2);
                String lastTx = cAll.getString(3);
                if ("CREDITOR".equals(pType)) {
                    if (bal > 0) { totalOnYou += bal; creditorsCount++; }
                } else {
                    if (bal > 0) {
                        totalForYou += bal;
                        debtorsCount++;
                        if (getDaysSince(lastTx) >= 30) overdueCount++;
                    } else if (bal < 0) {
                        totalOnYou += Math.abs(bal);
                        creditorsCount++;
                    }
                }
            }
            cAll.close();
        }

        tvTotalRemaining.setText(formatAmount(totalForYou));
        tvDebtorsCount.setText(debtorsCount + " زبوناً عليهم ديون");
        tvWhatYouOwe.setText("ما عليك: " + formatAmount(totalOnYou));

        String dateFilter = "";
        if (reportPeriod == 0) dateFilter = " WHERE created_at LIKE '" + todayStr + "%'";
        else if (reportPeriod == 1) {
            Calendar cal = Calendar.getInstance(); cal.add(Calendar.DAY_OF_YEAR, -7);
            String d7 = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.getTime());
            dateFilter = " WHERE created_at >= '" + d7 + "'";
        } else {
            String mStr = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
            dateFilter = " WHERE created_at LIKE '" + mStr + "%'";
        }

        double repDebts = 0, repPays = 0;
        Cursor cRep = db.getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type='DEBT' THEN amount ELSE 0 END),0), COALESCE(SUM(CASE WHEN type='PAYMENT' THEN amount ELSE 0 END),0) FROM transactions" + dateFilter, null);
        if (cRep != null) { if (cRep.moveToFirst()) { repDebts = cRep.getDouble(0); repPays = cRep.getDouble(1); } cRep.close(); }

        tvRepTotalRemaining.setText(formatAmount(totalForYou));
        tvRepTotalOwed.setText(formatAmount(totalOnYou));
        tvRepNewDebts.setText(formatAmount(repDebts));
        tvRepPayments.setText(formatAmount(repPays));
        tvRepDebtorsCount.setText(String.valueOf(debtorsCount));
        tvRepCreditorsCount.setText(String.valueOf(creditorsCount));
        tvRepOverdueCount.setText(String.valueOf(overdueCount));

        refreshQuickSlots();
        refreshCustomersList(etSearchCustomers.getText().toString());
        refreshQuickSelectList();
        refreshHomeLists();
        if (currentTab == 4 && selectedCustomerId != -1) loadCustomerDetail(selectedCustomerId);
    }

    private void openCustomerDetail(int customerId) { selectedCustomerId = customerId; switchTab(4); }

    private void loadCustomerDetail(int customerId) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.name, c.phone, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id WHERE c.id = ? GROUP BY c.id", new String[]{String.valueOf(customerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                selectedCustomerName = c.getString(0);
                selectedCustomerPhone = c.getString(1) != null ? c.getString(1) : "";
                selectedPartyType = c.getString(2) != null ? c.getString(2) : "CUSTOMER";
                selectedCustomerBalance = c.getDouble(3);
            }
            c.close();
        }
        boolean isCreditor = "CREDITOR".equals(selectedPartyType);
        tvDetailName.setText(selectedCustomerName);
        tvDetailPhone.setText(selectedCustomerPhone.isEmpty() ? "بدون رقم جوال" : selectedCustomerPhone);
        tvDetailStatusLabel.setText(isCreditor ? "المتبقي له عليك" : (selectedCustomerBalance >= 0 ? "المتبقي عليه" : "له رصيد عندك"));
        tvDetailBalance.setText(formatAmount(Math.abs(selectedCustomerBalance)));

        btnDetailAddDebt.setText(isCreditor ? "أخذت منه" : "أخذ منك");
        btnDetailAddPayment.setText(isCreditor ? "سدّدت له" : "سدّد لك");
        btnSettleAccount.setVisibility(selectedCustomerBalance != 0 ? View.VISIBLE : View.GONE);

        llDetailHistory.removeAllViews();
        Cursor h = db.getReadableDatabase().rawQuery("SELECT id, type, amount, note, created_at FROM transactions WHERE customer_id = ? ORDER BY id ASC", new String[]{String.valueOf(customerId)});
        ArrayList<View> rows = new ArrayList<>();
        double running = 0;
        if (h != null) {
            while (h.moveToNext()) {
                int txId = h.getInt(0);
                boolean isDebt = "DEBT".equals(h.getString(1));
                double amount = h.getDouble(2);
                String note = h.getString(3);
                String dateStr = h.getString(4);
                running += isDebt ? amount : -amount;
                final double afterBal = running;

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(24, 22, 24, 22);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#17201D"));
                bg.setCornerRadius(32f);
                bg.setStroke(2, Color.parseColor("#25332E"));
                row.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 12);
                row.setLayoutParams(lp);

                LinearLayout leftCol = new LinearLayout(this);
                leftCol.setOrientation(LinearLayout.VERTICAL);
                TextView tvAmt = new TextView(this);
                tvAmt.setText(formatAmount(amount));
                tvAmt.setTextColor(Color.WHITE);
                tvAmt.setTextSize(16f);
                TextView tvAfter = new TextView(this);
                tvAfter.setText("المتبقي بعده " + formatAmount(Math.abs(afterBal)));
                tvAfter.setTextColor(Color.parseColor("#93A8A1"));
                tvAfter.setTextSize(11f);
                leftCol.addView(tvAmt);
                leftCol.addView(tvAfter);

                LinearLayout infoCol = new LinearLayout(this);
                infoCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                ilp.setMargins(16, 0, 16, 0);
                infoCol.setLayoutParams(ilp);

                TextView tvTitle = new TextView(this);
                tvTitle.setText(isDebt ? (isCreditor ? "أخذت منه" : "أخذ منك") : (isCreditor ? "سدّدت له" : "سدّد لك"));
                tvTitle.setTextColor(Color.WHITE);
                tvTitle.setTextSize(15f);
                infoCol.addView(tvTitle);

                String sub = (dateStr != null ? dateStr : "") + ((note != null && !note.trim().isEmpty()) ? " • " + note : "");
                TextView tvDate = new TextView(this);
                tvDate.setText(sub);
                tvDate.setTextColor(Color.parseColor("#93A8A1"));
                tvDate.setTextSize(12f);
                infoCol.addView(tvDate);

                TextView badge = new TextView(this);
                badge.setText(isDebt ? "↑" : "↓");
                badge.setTextColor(Color.WHITE);
                badge.setGravity(Gravity.CENTER);
                GradientDrawable bBg = new GradientDrawable();
                bBg.setShape(GradientDrawable.OVAL);
                bBg.setColor(isDebt ? Color.parseColor("#D9534F") : Color.parseColor("#19C39D"));
                badge.setBackground(bBg);
                badge.setLayoutParams(new LinearLayout.LayoutParams(72, 72));

                row.addView(leftCol);
                row.addView(infoCol);
                row.addView(badge);
                row.setOnClickListener(v -> confirmDeleteTransaction(txId));
                rows.add(0, row);
            }
            h.close();
        }
        for (View v : rows) llDetailHistory.addView(v);
        if (rows.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("لا توجد حركات مسجلة حتى الآن.");
            empty.setTextColor(Color.parseColor("#93A8A1"));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(20, 40, 20, 40);
            llDetailHistory.addView(empty);
        }
    }

    private void confirmSettleAccount() {
        if (selectedCustomerBalance == 0) return;
        darkDialog().setTitle("تصفية الحساب").setMessage("هل تريد تسجيل حركة تصفية بقيمة " + formatAmount(Math.abs(selectedCustomerBalance)) + " ليصبح الرصيد صفراً؟")
                .setPositiveButton("نعم، صفّر الحساب", (d, w) -> {
                    String settleType = selectedCustomerBalance > 0 ? "PAYMENT" : "DEBT";
                    saveTransaction(selectedCustomerId, settleType, String.valueOf(Math.abs(selectedCustomerBalance)), "تصفية حساب");
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void confirmDeleteTransaction(int txId) {
        darkDialog().setTitle("حذف الحركة").setMessage("هل تريد حذف هذه الحركة؟")
                .setPositiveButton("حذف", (d, w) -> { db.getWritableDatabase().delete("transactions", "id = ?", new String[]{String.valueOf(txId)}); refreshAll(); })
                .setNegativeButton("إلغاء", null).show();
    }

    private void callSelectedCustomer() {
        if (selectedCustomerPhone.trim().isEmpty()) { Toast.makeText(this, "لا يوجد رقم هاتف مسجل", Toast.LENGTH_SHORT).show(); return; }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + selectedCustomerPhone.trim())));
    }

    private void openWhatsAppReminder() {
        String shop = prefs.getString("shop_owner", "المحل");
        String lastTxInfo = "";
        Cursor c = db.getReadableDatabase().rawQuery("SELECT type, amount, created_at FROM transactions WHERE customer_id = ? ORDER BY id DESC LIMIT 1", new String[]{String.valueOf(selectedCustomerId)});
        if (c != null) {
            if (c.moveToFirst()) {
                String tLabel = "DEBT".equals(c.getString(0)) ? "سحب" : "دفعة";
                lastTxInfo = " (آخر حركة: " + tLabel + " بقيمة " + formatAmount(c.getDouble(1)) + " بتاريخ " + c.getString(2) + ")";
            }
            c.close();
        }
        String defaultTpl = "مرحباً {الاسم}، معك {المحل}. نود تذكيركم بأن الرصيد المتبقي هو {المبلغ}{آخر_حركة}. شكراً لتعاملكم معنا.";
        String tpl = prefs.getString("reminder_tpl", defaultTpl);
        String msg = tpl.replace("{الاسم}", selectedCustomerName)
                .replace("{المحل}", shop)
                .replace("{المبلغ}", formatAmount(Math.abs(selectedCustomerBalance)))
                .replace("{آخر_حركة}", lastTxInfo);
        try {
            String phone = selectedCustomerPhone.replaceAll("[^0-9]", "");
            String url = phone.isEmpty() ? "https://wa.me/?text=" + URLEncoder.encode(msg, "UTF-8") : "https://wa.me/" + phone + "?text=" + URLEncoder.encode(msg, "UTF-8");
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) { Toast.makeText(this, "تعذر فتح واتساب", Toast.LENGTH_SHORT).show(); }
    }

    private void showEditReminderTemplateDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        String defaultTpl = "مرحباً {الاسم}، معك {المحل}. نود تذكيركم بأن الرصيد المتبقي هو {المبلغ}{آخر_حركة}. شكراً لتعاملكم معنا.";
        final EditText etTpl = styledEditText("نص الرسالة", prefs.getString("reminder_tpl", defaultTpl), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        layout.addView(etTpl);
        darkDialog().setTitle("تعديل قالب تذكير واتساب").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    prefs.edit().putString("reminder_tpl", etTpl.getText().toString().trim()).apply();
                    Toast.makeText(this, "تم حفظ القالب", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void shareCustomerStatement() {
        StringBuilder sb = new StringBuilder();
        sb.append("📄 كشف حساب: ").append(selectedCustomerName).append("\n");
        sb.append("المحل: ").append(prefs.getString("shop_owner", "المحل")).append("\n");
        sb.append("الرصيد الحالي: ").append(formatAmount(Math.abs(selectedCustomerBalance))).append("\n--------------------\n");
        Cursor h = db.getReadableDatabase().rawQuery("SELECT type, amount, note, created_at FROM transactions WHERE customer_id = ? ORDER BY id DESC", new String[]{String.valueOf(selectedCustomerId)});
        if (h != null) {
            while (h.moveToNext()) {
                sb.append("• ").append("DEBT".equals(h.getString(0)) ? "أخذ" : "سدد").append(": ").append(formatAmount(h.getDouble(1))).append(" | ").append(h.getString(3));
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
        sb.append("📊 تقرير دفتري - ").append(prefs.getString("shop_owner", "المحل")).append("\n");
        sb.append("إجمالي المتبقي لك: ").append(tvTotalRemaining.getText()).append("\n");
        sb.append(tvWhatYouOwe.getText()).append("\n--------------------\n");
        Cursor c = db.getReadableDatabase().rawQuery("SELECT c.name, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id ORDER BY bal DESC", null);
        if (c != null) {
            while (c.moveToNext()) {
                double bal = c.getDouble(2);
                boolean isCred = "CREDITOR".equals(c.getString(1));
                sb.append("• ").append(c.getString(0)).append(": ").append(formatAmount(Math.abs(bal))).append(isCred ? " (له عليك)" : (bal > 0 ? " (عليه)" : " (مسدد)")).append("\n");
            }
            c.close();
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, "مشاركة التقرير"));
    }

    private void showEditOrDeleteCustomerDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etName = styledEditText("الاسم", selectedCustomerName, InputType.TYPE_CLASS_TEXT);
        final EditText etPhone = styledEditText("رقم الهاتف", selectedCustomerPhone, InputType.TYPE_CLASS_PHONE);
        layout.addView(etName); layout.addView(etPhone);
        darkDialog().setTitle("تعديل البيانات").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    String n = etName.getText().toString().trim();
                    if (!n.isEmpty()) {
                        ContentValues cv = new ContentValues(); cv.put("name", n); cv.put("phone", etPhone.getText().toString().trim());
                        db.getWritableDatabase().update("customers", cv, "id = ?", new String[]{String.valueOf(selectedCustomerId)});
                        refreshAll();
                    }
                })
                .setNeutralButton("حذف نهائي", (d, w) -> {
                    db.getWritableDatabase().delete("transactions", "customer_id = ?", new String[]{String.valueOf(selectedCustomerId)});
                    db.getWritableDatabase().delete("customers", "id = ?", new String[]{String.valueOf(selectedCustomerId)});
                    switchTab(1);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void showWalletsDialog() {
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 24, 40, 10);
        final EditText etJawwal = styledEditText("رقم محفظة جوال باي", prefs.getString("w_jawwal", ""), InputType.TYPE_CLASS_PHONE);
        final EditText etPalpay = styledEditText("رقم محفظة بال باي", prefs.getString("w_palpay", ""), InputType.TYPE_CLASS_PHONE);
        final EditText etBank = styledEditText("رقم حساب بنك فلسطين", prefs.getString("w_bank", ""), InputType.TYPE_CLASS_TEXT);
        layout.addView(etJawwal); layout.addView(etPalpay); layout.addView(etBank);
        darkDialog().setTitle("المحافظ والبنوك").setView(layout)
                .setPositiveButton("حفظ", (d, w) -> {
                    prefs.edit().putString("w_jawwal", etJawwal.getText().toString().trim()).putString("w_palpay", etPalpay.getText().toString().trim()).putString("w_bank", etBank.getText().toString().trim()).apply();
                    Toast.makeText(this, "تم حفظ بيانات المحافظ", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("📤 إرسال للزبون", (d, w) -> {
                    String txt = "بيانات التحويل البنكي / المحافظ (" + prefs.getString("shop_owner", "المحل") + "):\n" +
                            "• جوال باي: " + etJawwal.getText().toString().trim() + "\n" +
                            "• بال باي: " + etPalpay.getText().toString().trim() + "\n" +
                            "• بنك فلسطين: " + etBank.getText().toString().trim();
                    Intent send = new Intent(Intent.ACTION_SEND);
                    send.setType("text/plain");
                    send.putExtra(Intent.EXTRA_TEXT, txt);
                    startActivity(Intent.createChooser(send, "إرسال أرقام المحافظ"));
                })
                .setNegativeButton("إغلاق", null).show();
    }

    private void refreshQuickSlots() {
        quickSlotCustomerIds.clear();
        Cursor c = db.getReadableDatabase().rawQuery("SELECT id, name FROM customers ORDER BY id DESC LIMIT 3", null);
        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        if (c != null) { while (c.moveToNext()) { ids.add(c.getInt(0)); names.add(c.getString(1)); } c.close(); }
        String[] defaultSymbols = {"ع", "أ", "م"};
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
                labels[i].setText("زبون");
            }
        }
    }

    private void handleQuickSlotClick(int slotIndex) {
        if (slotIndex < quickSlotCustomerIds.size() && quickSlotCustomerIds.get(slotIndex) != -1) openCustomerDetail(quickSlotCustomerIds.get(slotIndex));
        else showAddPartyDialog();
    }

    private void refreshCustomersList(String query) {
        llCustomersList.removeAllViews();
        float ceil = prefs.getFloat("debt_ceiling", 0f);
        String sql = "SELECT c.id, c.name, c.phone, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance, MAX(t.created_at) AS last_tx FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id ";
        String[] args = null;
        if (query != null && !query.trim().isEmpty()) {
            sql += "WHERE c.name LIKE ? OR c.phone LIKE ? ";
            String like = "%" + query.trim() + "%";
            args = new String[]{like, like};
        }
        sql += "GROUP BY c.id, c.name, c.phone, c.party_type ";
        if (custFilter == 1) sql += "HAVING balance > 0 AND c.party_type != 'CREDITOR' ";
        else if (custFilter == 2) sql += "HAVING c.party_type = 'CREDITOR' OR balance < 0 ";
        else if (custFilter == 3) sql += "HAVING c.party_type != 'CREDITOR' AND balance >= " + (ceil > 0 ? ceil : 99999999) + " ";

        sql += (sortByBalanceDesc ? "ORDER BY balance DESC, c.name ASC" : "ORDER BY c.name ASC");
        Cursor cursor = db.getReadableDatabase().rawQuery(sql, args);
        int count = 0;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                count++;
                llCustomersList.addView(buildPartyCardRow(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getDouble(4), cursor.getString(5)));
            }
            cursor.close();
        }
        tvCustomersTotalBadge.setText(count + " طرف");
    }

    private void refreshQuickSelectList() {
        llQuickSelectList.removeAllViews();
        Cursor cursor = db.getReadableDatabase().rawQuery("SELECT c.id, c.name, c.phone, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS balance, MAX(t.created_at) AS last_tx FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id GROUP BY c.id ORDER BY c.id DESC", null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                LinearLayout row = buildPartyCardRow(id, name, cursor.getString(2), cursor.getString(3), cursor.getDouble(4), cursor.getString(5));
                row.setOnClickListener(v -> showTransactionDialog(id, name, "DEBT"));
                llQuickSelectList.addView(row);
            }
            cursor.close();
        }
    }

    private LinearLayout buildPartyCardRow(int id, String name, String phone, String partyType, double balance, String lastTx) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(24, 22, 24, 22);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#17201D"));
        bg.setCornerRadius(36f);
        bg.setStroke(2, Color.parseColor("#25332E"));
        card.setBackground(bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, 12);
        card.setLayoutParams(cardParams);

        boolean isCreditor = "CREDITOR".equals(partyType);
        float ceil = prefs.getFloat("debt_ceiling", 0f);
        boolean overLimit = !isCreditor && ceil > 0 && balance >= ceil;
        long days = getDaysSince(lastTx);

        LinearLayout leftCol = new LinearLayout(this);
        leftCol.setOrientation(LinearLayout.VERTICAL);
        TextView tvBal = new TextView(this);
        if (balance == 0) {
            tvBal.setText("مسدّد بالكامل");
            tvBal.setTextColor(Color.parseColor("#93A8A1"));
            tvBal.setTextSize(13f);
        } else {
            tvBal.setText(formatAmount(Math.abs(balance)));
            tvBal.setTextColor(overLimit ? Color.parseColor("#FF6B6B") : Color.WHITE);
            tvBal.setTextSize(16f);
        }
        TextView tvStatus = new TextView(this);
        tvStatus.setText(balance == 0 ? "" : (isCreditor ? "له عليك" : (balance > 0 ? "عليه" : "له علي")));
        tvStatus.setTextColor(Color.parseColor("#93A8A1"));
        tvStatus.setTextSize(11f);
        leftCol.addView(tvBal);
        if (balance != 0) leftCol.addView(tvStatus);

        LinearLayout midCol = new LinearLayout(this);
        midCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams midParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        midParams.setMargins(16, 0, 16, 0);
        midCol.setLayoutParams(midParams);
        TextView tvName = new TextView(this);
        tvName.setText(name + (overLimit ? " ⚠️" : ""));
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(16f);
        midCol.addView(tvName);

        if (balance > 0 && days > 0) {
            TextView tvDelay = new TextView(this);
            tvDelay.setText("آخر حركة منذ " + days + " يوم");
            tvDelay.setTextColor(days >= 30 ? Color.parseColor("#FF6B6B") : Color.parseColor("#93A8A1"));
            tvDelay.setTextSize(11f);
            midCol.addView(tvDelay);
        }

        TextView tvAvatar = new TextView(this);
        tvAvatar.setText((name != null && !name.trim().isEmpty()) ? name.trim().substring(0, 1) : "ز");
        tvAvatar.setTextColor(Color.WHITE);
        tvAvatar.setTextSize(15f);
        tvAvatar.setGravity(Gravity.CENTER);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(isCreditor ? Color.parseColor("#3B2C1A") : Color.parseColor("#146350"));
        tvAvatar.setBackground(avatarBg);
        tvAvatar.setLayoutParams(new LinearLayout.LayoutParams(82, 82));

        card.addView(leftCol);
        card.addView(midCol);
        card.addView(tvAvatar);
        card.setOnClickListener(v -> openCustomerDetail(id));
        return card;
    }

    private void refreshHomeLists() {
        llBiggestOwed.removeAllViews();
        llHomeRecentTx.removeAllViews();

        Cursor cCred = db.getReadableDatabase().rawQuery("SELECT c.id, c.name, c.phone, c.party_type, COALESCE(SUM(CASE WHEN t.type = 'DEBT' THEN t.amount ELSE -t.amount END), 0) AS bal, MAX(t.created_at) FROM customers c LEFT JOIN transactions t ON c.id = t.customer_id WHERE c.party_type = 'CREDITOR' GROUP BY c.id HAVING bal > 0 ORDER BY bal DESC LIMIT 3", null);
        int credCount = 0;
        if (cCred != null) {
            while (cCred.moveToNext()) {
                credCount++;
                llBiggestOwed.addView(buildPartyCardRow(cCred.getInt(0), cCred.getString(1), cCred.getString(2), cCred.getString(3), cCred.getDouble(4), cCred.getString(5)));
            }
            cCred.close();
        }
        tvBiggestOwedHeader.setVisibility(credCount > 0 ? View.VISIBLE : View.GONE);

        String whereTx = "";
        if (homeTxFilter == 1) whereTx = " WHERE t.type = 'DEBT'";
        else if (homeTxFilter == 2) whereTx = " WHERE t.type = 'PAYMENT'";

        Cursor ct = db.getReadableDatabase().rawQuery("SELECT t.type, t.amount, t.note, t.created_at, c.name, c.id FROM transactions t JOIN customers c ON t.customer_id = c.id" + whereTx + " ORDER BY t.id DESC LIMIT 8", null);
        if (ct != null) {
            while (ct.moveToNext()) {
                boolean isDebt = "DEBT".equals(ct.getString(0));
                double amt = ct.getDouble(1);
                String note = ct.getString(2);
                String dateStr = ct.getString(3);
                String name = ct.getString(4);
                int custId = ct.getInt(5);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(24, 20, 24, 20);
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.parseColor("#17201D"));
                bg.setCornerRadius(30f);
                bg.setStroke(2, Color.parseColor("#25332E"));
                row.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 10);
                row.setLayoutParams(lp);

                TextView tvAmt = new TextView(this);
                tvAmt.setText(formatAmount(amt));
                tvAmt.setTextColor(Color.WHITE);
                tvAmt.setTextSize(15f);

                LinearLayout mid = new LinearLayout(this);
                mid.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                mlp.setMargins(16, 0, 16, 0);
                mid.setLayoutParams(mlp);
                TextView tvN = new TextView(this);
                tvN.setText(name); tvN.setTextColor(Color.WHITE); tvN.setTextSize(15f);
                TextView tvD = new TextView(this);
                tvD.setText((dateStr != null ? dateStr : "") + ((note != null && !note.trim().isEmpty()) ? " • " + note : ""));
                tvD.setTextColor(Color.parseColor("#93A8A1")); tvD.setTextSize(11f);
                mid.addView(tvN); mid.addView(tvD);

                TextView badge = new TextView(this);
                badge.setText(isDebt ? "↑" : "↓");
                badge.setTextColor(Color.WHITE);
                badge.setGravity(Gravity.CENTER);
                GradientDrawable bBg = new GradientDrawable();
                bBg.setShape(GradientDrawable.OVAL);
                bBg.setColor(isDebt ? Color.parseColor("#D9534F") : Color.parseColor("#19C39D"));
                badge.setBackground(bBg);
                badge.setLayoutParams(new LinearLayout.LayoutParams(68, 68));

                row.addView(tvAmt);
                row.addView(mid);
                row.addView(badge);
                row.setOnClickListener(v -> openCustomerDetail(custId));
                llHomeRecentTx.addView(row);
            }
            ct.close();
        }
    }

    private void showAddPartyDialog() {
        if (!isActivated()) {
            int count = 0;
            Cursor c = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers", null);
            if (c != null) { if (c.moveToFirst()) count = c.getInt(0); c.close(); }
            if (count >= 5) {
                Toast.makeText(this, "وصلت للحد التجريبي (5 أطراف)، فعّل النسخة الكاملة", Toast.LENGTH_LONG).show();
                layoutActivation.setVisibility(View.VISIBLE);
                return;
            }
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 10);

        final String[] selectedType = {"CUSTOMER"};
        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        trLp.setMargins(0, 0, 0, 16);
        toggleRow.setLayoutParams(trLp);

        TextView btnCust = new TextView(this);
        btnCust.setText("زبون (يأخذ منك)");
        btnCust.setGravity(Gravity.CENTER);
        btnCust.setPadding(16, 18, 16, 18);
        btnCust.setBackgroundResource(R.drawable.bg_btn_white);
        btnCust.setTextColor(Color.parseColor("#0E1412"));
        btnCust.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView btnCred = new TextView(this);
        btnCred.setText("دائن / تاجر (تأخذ منه)");
        btnCred.setGravity(Gravity.CENTER);
        btnCred.setPadding(16, 18, 16, 18);
        btnCred.setBackgroundResource(R.drawable.bg_btn_dark_outline);
        btnCred.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams crLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        crLp.setMargins(10, 0, 0, 0);
        btnCred.setLayoutParams(crLp);

        btnCust.setOnClickListener(v -> {
            selectedType[0] = "CUSTOMER";
            btnCust.setBackgroundResource(R.drawable.bg_btn_white); btnCust.setTextColor(Color.parseColor("#0E1412"));
            btnCred.setBackgroundResource(R.drawable.bg_btn_dark_outline); btnCred.setTextColor(Color.WHITE);
        });
        btnCred.setOnClickListener(v -> {
            selectedType[0] = "CREDITOR";
            btnCred.setBackgroundResource(R.drawable.bg_btn_white); btnCred.setTextColor(Color.parseColor("#0E1412"));
            btnCust.setBackgroundResource(R.drawable.bg_btn_dark_outline); btnCust.setTextColor(Color.WHITE);
        });

        toggleRow.addView(btnCust);
        toggleRow.addView(btnCred);
        layout.addView(toggleRow);

        final EditText etName = styledEditText("الاسم (مطلوب)", "", InputType.TYPE_CLASS_TEXT);
        final EditText etPhone = styledEditText("رقم الهاتف (اختياري)", "", InputType.TYPE_CLASS_PHONE);
        final EditText etNote = styledEditText("ملاحظات (اختياري)", "", InputType.TYPE_CLASS_TEXT);
        layout.addView(etName);
        layout.addView(etPhone);
        layout.addView(etNote);

        darkDialog().setTitle("إضافة طرف جديد").setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "يرجى إدخال الاسم", Toast.LENGTH_SHORT).show(); return; }
                    ContentValues values = new ContentValues();
                    values.put("name", name);
                    values.put("phone", etPhone.getText().toString().trim());
                    values.put("party_type", selectedType[0]);
                    values.put("note", etNote.getText().toString().trim());
                    long newId = db.getWritableDatabase().insert("customers", null, values);
                    refreshAll();
                    if (newId > 0) openCustomerDetail((int) newId);
                })
                .setNegativeButton("إلغاء", null).show();
    }

    private void showTransactionDialog(int customerId, String customerLabel, String type) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 24, 40, 10);

        final EditText etAmount = styledEditText("المبلغ (" + getCurrency() + ")", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        final EditText etNote = styledEditText("ملاحظة (اختياري)", "", InputType.TYPE_CLASS_TEXT);
        layout.addView(etAmount);

        LinearLayout amtRow = new LinearLayout(this);
        amtRow.setOrientation(LinearLayout.HORIZONTAL);
        int[] quickAmts = {10, 20, 50, 100};
        for (int qa : quickAmts) {
            TextView chip = new TextView(this);
            chip.setText("+" + qa);
            chip.setTextColor(Color.WHITE);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(12, 14, 12, 14);
            chip.setBackgroundResource(R.drawable.bg_btn_dark_outline);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(4, 0, 4, 12);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> {
                double cur = 0;
                try { String s = etAmount.getText().toString().trim(); if (!s.isEmpty()) cur = Double.parseDouble(s); } catch (Exception ignored) {}
                etAmount.setText(String.valueOf((int)(cur + qa)));
            });
            amtRow.addView(chip);
        }
        layout.addView(amtRow);
        layout.addView(etNote);

        // قوالب ملاحظات سريعة بضغطة زر
        LinearLayout noteRow = new LinearLayout(this);
        noteRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] quickNotes = "DEBT".equals(type) ? new String[]{"بضاعة", "طلبية", "سحب نقدي"} : new String[]{"نقداً", "تحويل", "دفعة حساب"};
        for (String qn : quickNotes) {
            TextView chip = new TextView(this);
            chip.setText(qn);
            chip.setTextColor(Color.parseColor("#9AF2DC"));
            chip.setTextSize(12f);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(10, 12, 10, 12);
            chip.setBackgroundResource(R.drawable.bg_pill_dark);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            clp.setMargins(4, 0, 4, 8);
            chip.setLayoutParams(clp);
            chip.setOnClickListener(v -> etNote.setText(qn));
            noteRow.addView(chip);
        }
        layout.addView(noteRow);

        String title = ("DEBT".equals(type) ? "تسجيل سحب / دين: " : "تسجيل تسديد: ") + customerLabel;
        darkDialog().setTitle(title).setView(layout)
                .setPositiveButton("حفظ", (dialog, which) -> checkCeilingAndSave(customerId, customerLabel, type, etAmount.getText().toString().trim(), etNote.getText().toString().trim()))
                .setNegativeButton("إلغاء", null).show();
    }

    private void checkCeilingAndSave(int customerId, String customerLabel, String type, String amountStr, String note) {
        if (amountStr.isEmpty()) { Toast.makeText(this, "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show(); return; }
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) { Toast.makeText(this, "المبلغ يجب أن يكون أكبر من صفر", Toast.LENGTH_SHORT).show(); return; }
        } catch (NumberFormatException e) { Toast.makeText(this, "صيغة المبلغ غير صحيحة", Toast.LENGTH_SHORT).show(); return; }

        float ceil = prefs.getFloat("debt_ceiling", 0f);
        if ("DEBT".equals(type) && ceil > 0) {
            double curBal = 0;
            String pType = "CUSTOMER";
            Cursor c = db.getReadableDatabase().rawQuery("SELECT c.party_type, COALESCE(SUM(CASE WHEN t.type='DEBT' THEN t.amount ELSE -t.amount END),0) FROM customers c LEFT JOIN transactions t ON c.id=t.customer_id WHERE c.id=? GROUP BY c.id", new String[]{String.valueOf(customerId)});
            if (c != null) { if (c.moveToFirst()) { pType = c.getString(0); curBal = c.getDouble(1); } c.close(); }
            if (!"CREDITOR".equals(pType) && (curBal + amount) > ceil) {
                darkDialog().setTitle("⚠️ تنبيه تجاوز سقف الدين")
                        .setMessage("رصيد " + customerLabel + " سيصبح " + formatAmount(curBal + amount) + " وسيتجاوز السقف المحدد (" + formatAmount(ceil) + "). هل تريد المتابعة؟")
                        .setPositiveButton("نعم، سجل الدين", (d, w) -> saveTransaction(customerId, type, amountStr, note))
                        .setNegativeButton("تراجع", null).show();
                return;
            }
        }
        saveTransaction(customerId, type, amountStr, note);
    }

    private void saveTransaction(int customerId, String type, String amountStr, String note) {
        try {
            double amount = Double.parseDouble(amountStr);
            ContentValues values = new ContentValues();
            values.put("customer_id", customerId);
            values.put("type", type);
            values.put("amount", amount);
            values.put("note", note);
            values.put("created_at", new SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.US).format(new Date()));
            db.getWritableDatabase().insert("transactions", null, values);
            if (currentTab == 5) openCustomerDetail(customerId);
            else refreshAll();
        } catch (Exception ignored) {}
    }

    private void showCalculatorDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(36, 24, 36, 12);

        final TextView display = new TextView(this);
        display.setText("0");
        display.setTextColor(Color.WHITE);
        display.setTextSize(32f);
        display.setGravity(Gravity.END);
        display.setPadding(24, 24, 24, 24);
        display.setBackgroundResource(R.drawable.bg_btn_dark_outline);
        layout.addView(display);

        final double[] firstVal = {0};
        final String[] op = {""};
        final boolean[] resetNext = {false};

        String[][] keys = {
                {"7", "8", "9", "÷"},
                {"4", "5", "6", "×"},
                {"1", "2", "3", "-"},
                {"C", "0", "=", "+"}
        };

        for (String[] rowKeys : keys) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp.setMargins(0, 10, 0, 0);
            row.setLayoutParams(rlp);

            for (String k : rowKeys) {
                TextView btn = new TextView(this);
                btn.setText(k);
                btn.setTextSize(20f);
                btn.setGravity(Gravity.CENTER);
                btn.setPadding(0, 22, 0, 22);
                boolean isOp = k.equals("+") || k.equals("-") || k.equals("×") || k.equals("÷");
                boolean isEq = k.equals("=");
                btn.setBackgroundResource(isEq ? R.drawable.bg_fab_center : (isOp ? R.drawable.bg_chip_active : R.drawable.bg_btn_dark_outline));
                btn.setTextColor((isOp || isEq) ? Color.parseColor("#0E1412") : Color.WHITE);
                LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                blp.setMargins(5, 0, 5, 0);
                btn.setLayoutParams(blp);

                btn.setOnClickListener(v -> {
                    String cur = display.getText().toString();
                    if (k.equals("C")) {
                        display.setText("0"); firstVal[0] = 0; op[0] = ""; resetNext[0] = false;
                    } else if (isOp) {
                        firstVal[0] = Double.parseDouble(cur);
                        op[0] = k;
                        resetNext[0] = true;
                    } else if (isEq) {
                        if (!op[0].isEmpty()) {
                            double second = Double.parseDouble(cur);
                            double res = firstVal[0];
                            if (op[0].equals("+")) res += second;
                            else if (op[0].equals("-")) res -= second;
                            else if (op[0].equals("×")) res *= second;
                            else if (op[0].equals("÷") && second != 0) res /= second;
                            display.setText(res == Math.floor(res) ? String.valueOf((long) res) : String.format(Locale.US, "%.2f", res));
                            op[0] = ""; resetNext[0] = true;
                        }
                    } else {
                        if (cur.equals("0") || resetNext[0]) { display.setText(k); resetNext[0] = false; }
                        else display.setText(cur + k);
                    }
                });
                row.addView(btn);
            }
            layout.addView(row);
        }

        darkDialog().setTitle("آلة حاسبة").setView(layout)
                .setPositiveButton("📋 نسخ الناتج", (d, w) -> {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("calc", display.getText().toString()));
                        Toast.makeText(this, "تم نسخ الناتج: " + display.getText(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("إغلاق", null).show();
    }
}