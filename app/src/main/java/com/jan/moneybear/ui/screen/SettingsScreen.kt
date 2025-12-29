package com.jan.moneybear.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jan.moneybear.MoneyBearApp
import com.jan.moneybear.R
import com.jan.moneybear.data.store.SavingsGoal
import com.jan.moneybear.domain.DEFAULT_EXPENSE_CATEGORIES
import com.jan.moneybear.domain.DEFAULT_INCOME_CATEGORIES
import com.jan.moneybear.ui.components.MoneyBearTopBarTitle
import com.jan.moneybear.util.LocaleUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyBearApp
    val settingsStore = app.settingsStore
    val transactionRepository = app.transactionRepository
    val authRepository = app.authRepository
    val scope = rememberCoroutineScope()

    val languageCode by settingsStore.languageCode.collectAsState(initial = "et")
    val currencyCode by settingsStore.currencyCode.collectAsState(initial = "EUR")
    val budgetMonthly by settingsStore.budgetMonthly.collectAsState(initial = null)
    val budgetCycleStartDay by settingsStore.budgetCycleStartDay.collectAsState(initial = 1)
    val themeMode by settingsStore.themeMode.collectAsState(initial = "dark")
    val accentColor by settingsStore.accentColor.collectAsState(initial = "teal")
    val savingsGoals by settingsStore.savingsGoals.collectAsState(initial = emptyList())
    val savingsBalances by transactionRepository.savingsBalances().collectAsState(initial = emptyMap())
    val balanceBaseline by settingsStore.balanceBaseline.collectAsState(initial = null)
    val expenseCategories by settingsStore.expenseCategories.collectAsState(
        initial = DEFAULT_EXPENSE_CATEGORIES
    )
    val incomeCategories by settingsStore.incomeCategories.collectAsState(
        initial = DEFAULT_INCOME_CATEGORIES
    )

    var langExpanded by remember { mutableStateOf(false) }
    var currExpanded by remember { mutableStateOf(false) }
    var startDayExpanded by remember { mutableStateOf(false) }
    var newExpenseCategory by remember { mutableStateOf("") }
    var newIncomeCategory by remember { mutableStateOf("") }
    var newSavingsGoalName by remember { mutableStateOf("") }
    var newSavingsGoalTarget by remember { mutableStateOf("") }
    var newSavingsGoalDeadline by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var baselineAmountInput by remember(balanceBaseline) {
        mutableStateOf(
            balanceBaseline?.amount?.let { String.format(Locale.getDefault(), "%.2f", abs(it)) } ?: ""
        )
    }
    var baselineDateMillis by remember(balanceBaseline) { mutableStateOf(balanceBaseline?.dateMillis) }
    var showBaselineDatePicker by remember { mutableStateOf(false) }
    var goalBeingEdited by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalPendingDeletion by remember { mutableStateOf<SavingsGoal?>(null) }
    var categoryPendingDeletion by remember { mutableStateOf<CategoryDeletionRequest?>(null) }
    val scrollState = rememberScrollState()

    val languages = remember {
        listOf(
            LanguageOption("et", "🇪🇪", "Eesti", "Estonian"),
            LanguageOption("en", "🇬🇧", "English", "English"),
            LanguageOption("fi", "🇫🇮", "Suomi", "Finnish"),
            LanguageOption("sv", "🇸🇪", "Svenska", "Swedish"),
            LanguageOption("ru", "🇷🇺", "Русский", "Russian"),
            LanguageOption("lv", "🇱🇻", "Latviešu", "Latvian"),
            LanguageOption("lt", "🇱🇹", "Lietuvių", "Lithuanian")
        )
    }
    val currencies = remember {
        listOf(
            CurrencyOption("EUR", "€", "Euro"),
            CurrencyOption("USD", "$", "US Dollar"),
            CurrencyOption("GBP", "£", "British Pound"),
            CurrencyOption("SEK", "kr", "Swedish Krona"),
            CurrencyOption("NOK", "kr", "Norwegian Krone"),
            CurrencyOption("DKK", "kr", "Danish Krone"),
            CurrencyOption("PLN", "zł", "Polish Złoty"),
            CurrencyOption("RUB", "₽", "Russian Ruble")
        )
    }
    val accentOptions = remember {
        listOf(
            AccentOption("teal", R.string.accent_teal_label, Color(0xFF00BFA5)),
            AccentOption("orange", R.string.accent_orange_label, Color(0xFFFF9800)),
            AccentOption("blue", R.string.accent_blue_label, Color(0xFF2962FF)),
            AccentOption("purple", R.string.accent_purple_label, Color(0xFF8E24AA)),
            AccentOption("green", R.string.accent_green_label, Color(0xFF2E7D32))
        )
    }

    var budgetInput by remember(budgetMonthly) {
        mutableStateOf(budgetMonthly?.let { String.format(Locale.getDefault(), "%.2f", abs(it)) } ?: "")
    }
    val budgetError = budgetInput.isNotEmpty() && budgetInput.replace(',', '.').toDoubleOrNull() == null
    fun addExpenseCategory() {
        val entry = newExpenseCategory.trim()
        if (entry.isEmpty() || expenseCategories.any { it.equals(entry, ignoreCase = true) }) return
        scope.launch {
            settingsStore.setExpenseCategories(expenseCategories + entry)
            newExpenseCategory = ""
        }
    }

    fun addIncomeCategory() {
        val entry = newIncomeCategory.trim()
        if (entry.isEmpty() || incomeCategories.any { it.equals(entry, ignoreCase = true) }) return
        scope.launch {
            settingsStore.setIncomeCategories(incomeCategories + entry)
            newIncomeCategory = ""
        }
    }

    fun removeExpenseCategory(category: String) {
        scope.launch {
            settingsStore.setExpenseCategories(expenseCategories.filterNot { it.equals(category, ignoreCase = true) })
        }
    }

    fun removeIncomeCategory(category: String) {
        scope.launch {
            settingsStore.setIncomeCategories(incomeCategories.filterNot { it.equals(category, ignoreCase = true) })
        }
    }

    fun persistBudget() {
        val parsed = budgetInput.replace(',', '.').toDoubleOrNull()
        scope.launch {
            if (parsed != null) {
                settingsStore.setBudgetMonthly(abs(parsed))
            } else {
                settingsStore.setBudgetMonthly(null)
            }
        }
    }

    val selectedLanguage = languages.firstOrNull { it.code == languageCode }
    val selectedCurrency = currencies.firstOrNull { it.code == currencyCode }
    val isDarkMode = themeMode != "light"
    val cycleDayOptions = remember { (1..31).toList() }
    val selectedCycleLabel = if (budgetCycleStartDay == 1) {
        stringResource(R.string.budget_cycle_day_first)
    } else {
        stringResource(R.string.budget_cycle_day_number, budgetCycleStartDay)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    MoneyBearTopBarTitle(subtitle = stringResource(R.string.settings))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSectionCard(titleRes = R.string.preferences) {
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = !langExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLanguage?.displayName ?: languageCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.language)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )

                    DropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    scope.launch {
                                        settingsStore.setLanguage(lang.code)
                                        LocaleUtils.applyAppLanguage(lang.code)
                                    }
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = currExpanded,
                    onExpandedChange = { currExpanded = !currExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrency?.formatted ?: currencyCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.currency)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )

                    DropdownMenu(
                        expanded = currExpanded,
                        onDismissRequest = { currExpanded = false }
                    ) {
                        currencies.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr.formatted) },
                                onClick = {
                                    scope.launch { settingsStore.setCurrency(curr.code) }
                                    currExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            SettingsSectionCard(titleRes = R.string.settings_budget_section_title) {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text(stringResource(R.string.monthly_budget_label)) },
                    isError = budgetError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (!budgetError) persistBudget()
                    }),
                    trailingIcon = { Text(selectedCurrency?.symbol ?: currencyCode) },
                    supportingText = {
                        if (budgetError) {
                            Text(stringResource(R.string.monthly_budget_error))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { if (!budgetError) persistBudget() },
                    enabled = !budgetError,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_budget))
                }
                ExposedDropdownMenuBox(
                    expanded = startDayExpanded,
                    onExpandedChange = { startDayExpanded = !startDayExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCycleLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.budget_cycle_start_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDayExpanded) },
                        supportingText = {
                            Text(stringResource(R.string.budget_cycle_helper))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    DropdownMenu(
                        expanded = startDayExpanded,
                        onDismissRequest = { startDayExpanded = false }
                    ) {
                        cycleDayOptions.forEach { day ->
                            val optionLabel = if (day == 1) {
                                stringResource(R.string.budget_cycle_day_first)
                            } else {
                                stringResource(R.string.budget_cycle_day_number, day)
                            }
                            DropdownMenuItem(
                                text = { Text(optionLabel) },
                                onClick = {
                                    startDayExpanded = false
                                    scope.launch {
                                        settingsStore.setBudgetCycleStartDay(day)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            SettingsSectionCard(titleRes = R.string.balance_baseline_section_title) {
                val baselineAmountError =
                    baselineAmountInput.isNotEmpty() && baselineAmountInput.replace(',', '.').toDoubleOrNull() == null
                OutlinedTextField(
                    value = baselineAmountInput,
                    onValueChange = { input ->
                        baselineAmountInput = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text(stringResource(R.string.balance_baseline_amount_label)) },
                    isError = baselineAmountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    trailingIcon = { Text(selectedCurrency?.symbol ?: currencyCode) },
                    supportingText = {
                        Text(stringResource(R.string.balance_baseline_helper))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                val baselineDateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                val baselineDateText = baselineDateMillis?.let { baselineDateFormat.format(it) } ?: ""
                DateSelectionField(
                    value = baselineDateText,
                    label = stringResource(R.string.balance_baseline_date_label),
                    showClear = baselineDateMillis != null,
                    onClear = { baselineDateMillis = null },
                    onClick = { showBaselineDatePicker = true }
                )
                Button(
                    onClick = {
                        val parsedAmount = baselineAmountInput.replace(',', '.').toDoubleOrNull()
                        val date = baselineDateMillis
                        if (parsedAmount != null && date != null) {
                            scope.launch {
                                settingsStore.setBalanceBaseline(abs(parsedAmount), date)
                            }
                        }
                    },
                    enabled = !baselineAmountError && baselineDateMillis != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.balance_baseline_save))
                }
                if (balanceBaseline != null) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                settingsStore.clearBalanceBaseline()
                                baselineAmountInput = ""
                                baselineDateMillis = null
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.balance_baseline_clear))
                    }
                }
            }

            SettingsSectionCard(titleRes = R.string.savings_section_title) {
                if (savingsGoals.isEmpty()) {
                    Text(
                        text = stringResource(R.string.savings_goals_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        savingsGoals.forEach { goal ->
                            SavingsGoalRow(
                                goal = goal,
                                balance = savingsBalances[goal.id] ?: 0.0,
                                currency = selectedCurrency?.symbol ?: currencyCode,
                                onEdit = { goalBeingEdited = it },
                                onDelete = { goalPendingDeletion = it }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = newSavingsGoalName,
                    onValueChange = { newSavingsGoalName = it },
                    label = { Text(stringResource(R.string.savings_goal_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                val goalTargetError = newSavingsGoalTarget.isNotEmpty() && newSavingsGoalTarget.replace(',', '.').toDoubleOrNull() == null
                OutlinedTextField(
                    value = newSavingsGoalTarget,
                    onValueChange = { input ->
                        newSavingsGoalTarget = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text(stringResource(R.string.savings_goal_target_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = goalTargetError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    trailingIcon = { Text(selectedCurrency?.symbol ?: currencyCode) },
                    supportingText = {
                        Text(
                            text = if (goalTargetError) {
                                stringResource(R.string.monthly_budget_error)
                            } else {
                                stringResource(R.string.savings_goal_helper)
                            }
                        )
                    }
                )
                val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                val deadlineText = newSavingsGoalDeadline?.let { dateFormat.format(it) } ?: ""
                DateSelectionField(
                    value = deadlineText,
                    label = stringResource(R.string.savings_goal_deadline_label),
                    helperText = stringResource(R.string.savings_goal_deadline_helper),
                    showClear = newSavingsGoalDeadline != null,
                    onClear = { newSavingsGoalDeadline = null },
                    onClick = { showDatePicker = true }
                )
                Button(
                    onClick = {
                        val targetValue = newSavingsGoalTarget.replace(',', '.').toDoubleOrNull()
                        if (targetValue != null) {
                            scope.launch {
                                settingsStore.addSavingsGoal(newSavingsGoalName, abs(targetValue), newSavingsGoalDeadline)
                                newSavingsGoalName = ""
                                newSavingsGoalTarget = ""
                                newSavingsGoalDeadline = null
                            }
                        }
                    },
                    enabled = newSavingsGoalName.trim().isNotEmpty() && newSavingsGoalTarget.replace(',', '.').toDoubleOrNull()?.let { it >= 0.0 } == true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.add_savings_goal))
                }
            }

            SettingsSectionCard(titleRes = R.string.settings_categories_section_title) {
                Text(
                    text = stringResource(R.string.expense),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryChipSection(
                    categories = expenseCategories,
                    onRemove = { category ->
                        categoryPendingDeletion = CategoryDeletionRequest(category, CategoryType.EXPENSE)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newExpenseCategory,
                        onValueChange = { newExpenseCategory = it },
                        label = { Text(stringResource(R.string.new_expense_category_hint)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Button(
                        onClick = { addExpenseCategory() },
                        enabled = newExpenseCategory.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_category))
                    }
                }

                Text(
                    text = stringResource(R.string.income),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CategoryChipSection(
                    categories = incomeCategories,
                    onRemove = { category ->
                        categoryPendingDeletion = CategoryDeletionRequest(category, CategoryType.INCOME)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newIncomeCategory,
                        onValueChange = { newIncomeCategory = it },
                        label = { Text(stringResource(R.string.new_income_category_hint)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    Button(
                        onClick = { addIncomeCategory() },
                        enabled = newIncomeCategory.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.add_category))
                    }
                }
            }

            SettingsSectionCard(titleRes = R.string.appearance_section_title) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (isDarkMode) stringResource(R.string.theme_dark_label) else stringResource(R.string.theme_light_label),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(R.string.theme_toggle_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { checked ->
                            val next = if (checked) "dark" else "light"
                            scope.launch { settingsStore.setThemeMode(next) }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                    )
                }

                Text(
                    text = stringResource(R.string.accent_color_label),
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    accentOptions.forEach { option ->
                        AccentOptionChip(
                            option = option,
                            selected = option.id == accentColor,
                            onSelected = {
                                scope.launch { settingsStore.setAccentColor(option.id) }
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        authRepository.signOut()
                        onLogout()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.log_out))
            }
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        newSavingsGoalDeadline?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                newSavingsGoalDeadline = selectedCalendar.timeInMillis
                showDatePicker = false
            },
            year,
            month,
            day
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }

    if (showBaselineDatePicker) {
        val calendar = Calendar.getInstance()
        baselineDateMillis?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                baselineDateMillis = selectedCalendar.timeInMillis
                showBaselineDatePicker = false
            },
            year,
            month,
            day
        ).apply {
            setOnDismissListener { showBaselineDatePicker = false }
            show()
        }
    }

    goalBeingEdited?.let { goal ->
        EditSavingsGoalDialog(
            goal = goal,
            currency = selectedCurrency?.symbol ?: currencyCode,
            onDismiss = { goalBeingEdited = null },
            onSave = { name, target, deadline ->
                scope.launch {
                    settingsStore.updateSavingsGoal(goal.id, name, target, deadline)
                    goalBeingEdited = null
                }
            }
        )
    }

    goalPendingDeletion?.let { goal ->
        DeleteSavingsGoalDialog(
            goal = goal,
            onConfirm = {
                scope.launch {
                    settingsStore.deleteSavingsGoal(goal.id)
                    goalPendingDeletion = null
                }
            },
            onDismiss = { goalPendingDeletion = null }
        )
    }

    categoryPendingDeletion?.let { pending ->
        AlertDialog(
            onDismissRequest = { categoryPendingDeletion = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = {
                Text(
                    text = stringResource(R.string.delete_category_message, pending.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (pending.type) {
                            CategoryType.EXPENSE -> removeExpenseCategory(pending.name)
                            CategoryType.INCOME -> removeIncomeCategory(pending.name)
                        }
                        categoryPendingDeletion = null
                    }
                ) {
                    Text(stringResource(R.string.delete_category_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryPendingDeletion = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

}

@Composable
private fun SavingsGoalRow(
    goal: SavingsGoal,
    balance: Double,
    currency: String,
    onEdit: (SavingsGoal) -> Unit,
    onDelete: (SavingsGoal) -> Unit
) {
    val safeBalance = balance.coerceAtLeast(0.0)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { onEdit(goal) }) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.edit_savings_goal)
                        )
                    }
                    IconButton(onClick = { onDelete(goal) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_savings_goal)
                        )
                    }
                }
            }
            Text(
                text = stringResource(
                    R.string.savings_goal_progress_label,
                    formatAmount(safeBalance, currency),
                    formatAmount(goal.target, currency)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditSavingsGoalDialog(
    goal: SavingsGoal,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long?) -> Unit
) {
    val context = LocalContext.current
    var name by remember(goal) { mutableStateOf(goal.name) }
    var target by remember(goal) {
        mutableStateOf(String.format(Locale.getDefault(), "%.2f", goal.target))
    }
    var deadline by remember(goal) { mutableStateOf(goal.deadlineMillis) }
    var showDatePicker by remember { mutableStateOf(false) }
    val targetError = target.isNotEmpty() && target.replace(',', '.').toDoubleOrNull() == null
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val deadlineText = deadline?.let { dateFormat.format(it) } ?: ""
    
    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        deadline?.let { calendar.timeInMillis = it }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        android.app.DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                deadline = selectedCalendar.timeInMillis
                showDatePicker = false
            },
            year,
            month,
            day
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.show()
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_savings_goal)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.savings_goal_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { input ->
                        target = input.filter { it.isDigit() || it == '.' || it == ',' }
                    },
                    label = { Text(stringResource(R.string.savings_goal_target_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = targetError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    trailingIcon = { Text(currency) },
                    supportingText = {
                        Text(
                            text = if (targetError) {
                                stringResource(R.string.monthly_budget_error)
                            } else {
                                stringResource(R.string.savings_goal_helper)
                            }
                        )
                    }
                )
                OutlinedTextField(
                    value = deadlineText,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.savings_goal_deadline_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    trailingIcon = {
                        if (deadline != null) {
                            IconButton(onClick = { deadline = null }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                            }
                        }
                    },
                    supportingText = {
                        Text(stringResource(R.string.savings_goal_deadline_helper))
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetValue = target.replace(',', '.').toDoubleOrNull()
                    if (targetValue != null) {
                        onSave(name, abs(targetValue), deadline)
                    }
                },
                enabled = name.trim().isNotEmpty() && !targetError && target.isNotEmpty()
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DateSelectionField(
    value: String,
    label: String,
    helperText: String? = null,
    showClear: Boolean = false,
    onClear: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isNotBlank()) value else stringResource(R.string.pick_date),
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isNotBlank()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showClear && onClear != null) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.clear))
                    }
                }
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = stringResource(R.string.pick_date)
                )
            }
        }
        helperText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DeleteSavingsGoalDialog(
    goal: SavingsGoal,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_savings_goal)) },
        text = {
            Text(
                text = stringResource(R.string.delete_savings_goal_confirm, goal.name),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_savings_goal))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatAmount(amount: Double, currency: String): String =
    String.format(Locale.getDefault(), "%.2f %s", abs(amount), currency)

@Composable
private fun SettingsSectionCard(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge
            )
            content()
        }
    }
}

@Composable
private fun AccentOptionChip(
    option: AccentOption,
    selected: Boolean,
    onSelected: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onSelected,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(option.color)
                )
                Text(text = stringResource(option.labelRes))
            }
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = option.color.copy(alpha = 0.3f),
            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipSection(
    categories: List<String>,
    onRemove: (String) -> Unit
) {
    if (categories.isEmpty()) {
        Text(
            text = stringResource(R.string.no_category_available),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                AssistChip(
                    onClick = { onRemove(category) },
                    label = { Text(category) },
                    leadingIcon = {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                )
            }
        }
    }
}

private enum class CategoryType {
    EXPENSE,
    INCOME
}

private data class CategoryDeletionRequest(
    val name: String,
    val type: CategoryType
)

private data class LanguageOption(
    val code: String,
    val flag: String,
    val localName: String,
    val englishName: String
) {
    val displayName: String get() = "$flag $localName / $englishName"
}

private data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String
) {
    val formatted: String get() = "$symbol $code - $name"
}

private data class AccentOption(
    val id: String,
    @StringRes val labelRes: Int,
    val color: Color
)
