package com.jan.moneybear.ui.screen

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jan.moneybear.MoneyBearApp
import com.jan.moneybear.R
import com.jan.moneybear.data.local.Transaction
import com.jan.moneybear.data.local.TxType
import com.jan.moneybear.domain.defaultExpenseCategories
import com.jan.moneybear.domain.defaultIncomeCategories
import com.jan.moneybear.domain.TransactionRepository
import com.jan.moneybear.domain.monthKey
import com.jan.moneybear.domain.newTxId
import com.jan.moneybear.ui.components.MoneyBearTopBarTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.launch

private enum class ScheduleType {
    SINGLE,
    RECURRING
}

private enum class EntryTab {
    EXPENSE,
    INCOME,
    SAVINGS
}

private enum class SavingsDirection {
    ADD,
    WITHDRAW
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionScreen(
    onNavigateBack: () -> Unit,
    transactionId: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyBearApp
    val transactionRepository: TransactionRepository = app.transactionRepository
    val authRepository = app.authRepository
    val settingsStore = app.settingsStore
    val scope = rememberCoroutineScope()

    val defaultExpense = defaultExpenseCategories(context)
    val defaultIncome = defaultIncomeCategories(context)
    val expenseCategories by settingsStore.expenseCategories.collectAsState(
        initial = defaultExpense
    )
    val incomeCategories by settingsStore.incomeCategories.collectAsState(
        initial = defaultIncome
    )
    val savingsGoals by settingsStore.savingsGoals.collectAsState(initial = emptyList())
    val currency by settingsStore.currencyCode.collectAsState(initial = "EUR")

    val existingTransactionFlow = remember(transactionId) {
        transactionId?.let { transactionRepository.observeTransaction(it) }
    }
    val existingTransaction by existingTransactionFlow?.collectAsState(initial = null)
        ?: remember { mutableStateOf<Transaction?>(null) }
    var hasInitialized by rememberSaveable(transactionId) { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(TxType.EXPENSE) }
    var selectedTab by rememberSaveable { mutableStateOf(EntryTab.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var scheduleType by remember { mutableStateOf(ScheduleType.SINGLE) }
    var recurringMonths by remember { mutableStateOf("1") }
    var selectedGoalId by remember { mutableStateOf<String?>(null) }
    var savingsImpactInput by remember { mutableStateOf("") }
    var savingsDirection by rememberSaveable { mutableStateOf(SavingsDirection.ADD) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val formattedDate = remember(dateMillis) { dateFormatter.format(dateMillis) }

    val recentTransactions by transactionRepository.listRecent(200).collectAsState(initial = emptyList())
    val categoriesForType = if (selectedType == TxType.EXPENSE) expenseCategories else incomeCategories
    val orderedCategories = remember(categoriesForType, recentTransactions, selectedType) {
        if (categoriesForType.isEmpty()) {
            categoriesForType
        } else {
            val counts = recentTransactions
                .filter { it.type == selectedType }
                .groupingBy { it.category.trim().lowercase(Locale.getDefault()) }
                .eachCount()
            val baseOrder = categoriesForType.mapIndexed { index, cat -> cat to index }.toMap()
            categoriesForType.sortedWith(
                compareByDescending<String> { counts[it.trim().lowercase(Locale.getDefault())] ?: 0 }
                    .thenBy { baseOrder[it] ?: Int.MAX_VALUE }
            )
        }
    }

    fun formatDoubleInput(value: Double): String =
        String.format(Locale.getDefault(), "%.2f", abs(value))

    LaunchedEffect(existingTransaction, savingsGoals) {
        val tx = existingTransaction
        if (!hasInitialized && tx != null) {
            val isSavingsEntry = tx.savingsGoalId != null && tx.amount == 0.0
            selectedType = tx.type
            selectedTab = when {
                isSavingsEntry -> EntryTab.SAVINGS
                tx.type == TxType.INCOME -> EntryTab.INCOME
                else -> EntryTab.EXPENSE
            }
            amount = formatDoubleInput(tx.amount)
            category = tx.category
            note = tx.note.orEmpty()
            dateMillis = tx.dateUtcMillis
            scheduleType = ScheduleType.SINGLE
            val goalId = tx.savingsGoalId?.takeIf { id -> savingsGoals.any { it.id == id } }
            if (isSavingsEntry && goalId != null) {
                selectedGoalId = goalId
                savingsImpactInput = formatDoubleInput(abs(tx.savingsImpact))
                savingsDirection = if (tx.savingsImpact < 0) {
                    SavingsDirection.WITHDRAW
                } else {
                    SavingsDirection.ADD
                }
            } else {
                selectedGoalId = null
                savingsImpactInput = ""
                savingsDirection = SavingsDirection.ADD
            }
            hasInitialized = true
        }
    }

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                dateMillis = cal.timeInMillis
                showDatePicker = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { showDatePicker = false }
        }.also { dialog ->
            dialog.show()
        }
    }

    val scheduleControlsEnabled = transactionId == null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    val titleRes = if (transactionId == null) {
                        R.string.add_transaction
                    } else {
                        R.string.edit_transaction
                    }
                    MoneyBearTopBarTitle(subtitle = stringResource(titleRes))
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val cardShape = RoundedCornerShape(20.dp)
            val savingsEnabled = savingsGoals.isNotEmpty()
            val amountValue = amount.replace(',', '.').toDoubleOrNull()
            val recurringCountValue = recurringMonths.toIntOrNull() ?: 0
            val isSavingsTab = selectedTab == EntryTab.SAVINGS
            val scheduleValid = when (scheduleType) {
                ScheduleType.SINGLE -> true
                ScheduleType.RECURRING -> recurringCountValue >= 0
            }
            val savingsImpactValue = savingsImpactInput.replace(',', '.').toDoubleOrNull()
            val savingsImpactError = savingsImpactInput.isNotEmpty() &&
                (savingsImpactInput.replace(',', '.').toDoubleOrNull()?.let { it <= 0.0 } != false)
            val trimmedCategory = category.trim()
            val categorySelected = if (categoriesForType.isEmpty()) {
                trimmedCategory.isNotEmpty()
            } else if (transactionId != null) {
                trimmedCategory.isNotEmpty()
            } else {
                categoriesForType.any { it.equals(trimmedCategory, ignoreCase = true) }
            }
            val regularFormValid = amountValue != null &&
                amountValue > 0.0 &&
                categorySelected &&
                scheduleValid
            val savingsFormValid = savingsEnabled &&
                selectedGoalId != null &&
                savingsImpactValue != null &&
                savingsImpactValue > 0.0
            val formValid = if (isSavingsTab) savingsFormValid else regularFormValid
            val emptyCategoryLabel = stringResource(R.string.category)
            val savingsEntryFallbackCategory = stringResource(R.string.savings_entry_category_fallback)

            fun handleTransactionSave(forType: TxType) {
                if (!formValid) return
                scope.launch {
                    val uid = existingTransaction?.uid
                        ?: authRepository.currentUser?.uid
                        ?: ""

                    val normalizedCategory = trimmedCategory.ifBlank { emptyCategoryLabel }
                    val baseId = existingTransaction?.id ?: newTxId()
                    val plannedFlag = if (transactionId == null) {
                        scheduleType == ScheduleType.RECURRING
                    } else {
                        existingTransaction?.planned ?: false
                    }
                    val amountToPersist = amountValue ?: 0.0

                    val baseTransaction = (existingTransaction ?: Transaction(
                        id = baseId,
                        uid = uid,
                        amount = amountToPersist,
                        currency = currency,
                        dateUtcMillis = dateMillis,
                        monthKey = monthKey(dateMillis),
                        category = normalizedCategory,
                        note = note.ifBlank { null },
                        planned = plannedFlag,
                        type = forType
                    )).copy(
                        id = baseId,
                        uid = uid,
                        amount = amountToPersist,
                        currency = currency,
                        dateUtcMillis = dateMillis,
                        monthKey = monthKey(dateMillis),
                        category = normalizedCategory,
                        note = note.ifBlank { null },
                        planned = plannedFlag,
                        type = forType,
                        savingsGoalId = null,
                        savingsImpact = 0.0
                    )

                    val toPersist = when {
                        transactionId != null -> listOf(baseTransaction)
                        scheduleType == ScheduleType.SINGLE -> listOf(baseTransaction)
                        scheduleType == ScheduleType.RECURRING -> buildRecurringTransactions(
                            base = baseTransaction,
                            repeatCount = recurringCountValue,
                            impactPerOccurrence = 0.0
                        )
                        else -> listOf(baseTransaction)
                    }

                    toPersist.forEach { transaction ->
                        transactionRepository.addOrUpdate(transaction)
                    }
                    onNavigateBack()
                }
            }

            fun handleSavingsEntrySave() {
                val goalId = selectedGoalId ?: return
                val impactValue = savingsImpactValue ?: return
                if (impactValue <= 0.0) return
                scope.launch {
                    val uid = existingTransaction?.uid
                        ?: authRepository.currentUser?.uid
                        ?: ""
                    val normalizedCategory = savingsGoals
                        .firstOrNull { it.id == goalId }
                        ?.name
                        ?: savingsEntryFallbackCategory
                    val signedImpact = if (savingsDirection == SavingsDirection.WITHDRAW) {
                        -abs(impactValue)
                    } else {
                        abs(impactValue)
                    }
                    val savingsType = if (savingsDirection == SavingsDirection.WITHDRAW) {
                        TxType.EXPENSE
                    } else {
                        TxType.INCOME
                    }
                    val savingsTransaction = Transaction(
                        id = newTxId(),
                        uid = uid,
                        amount = 0.0,
                        currency = currency,
                        dateUtcMillis = dateMillis,
                        monthKey = monthKey(dateMillis),
                        category = normalizedCategory,
                        note = note.ifBlank { null },
                        planned = false,
                        type = savingsType,
                        savingsGoalId = goalId,
                        savingsImpact = signedImpact
                    )
                    transactionRepository.addOrUpdate(savingsTransaction)
                    onNavigateBack()
                }
            }

            val canSaveSavingsEntry = savingsFormValid

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = cardShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val tabs = listOf(
                        EntryTab.EXPENSE to stringResource(R.string.expense),
                        EntryTab.INCOME to stringResource(R.string.income),
                        EntryTab.SAVINGS to stringResource(R.string.savings_section_title)
                    )
                    val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)
                    TabRow(selectedTabIndex = selectedIndex) {
                        tabs.forEachIndexed { index, (tab, label) ->
                            Tab(
                                selected = selectedIndex == index,
                                onClick = {
                                    if (selectedTab != tab) {
                                        selectedTab = tab
                                        if (tab == EntryTab.EXPENSE) {
                                            selectedType = TxType.EXPENSE
                                            category = ""
                                        } else if (tab == EntryTab.INCOME) {
                                            selectedType = TxType.INCOME
                                            category = ""
                                        }
                                    }
                                },
                                text = {
                                    Text(
                                        text = label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }

                    if (!isSavingsTab) {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { input ->
                                amount = input.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            label = { Text(stringResource(R.string.amount)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            ),
                            suffix = { Text(currency) }
                        )
                    } else {
                        OutlinedTextField(
                            value = savingsImpactInput,
                            onValueChange = { input ->
                                savingsImpactInput = input.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            label = { Text(stringResource(R.string.amount)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = savingsImpactError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done
                            ),
                            supportingText = {
                                Text(stringResource(R.string.savings_amount_helper))
                            },
                            suffix = { Text(currency) }
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.date), style = MaterialTheme.typography.titleSmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(formattedDate, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Filled.CalendarToday,
                                    contentDescription = stringResource(R.string.pick_date)
                                )
                            }
                        }
                    }

                    if (!isSavingsTab) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = scheduleType == ScheduleType.RECURRING,
                                    onClick = {
                                        if (scheduleControlsEnabled) {
                                            scheduleType = if (scheduleType == ScheduleType.RECURRING) {
                                                ScheduleType.SINGLE
                                            } else {
                                                ScheduleType.RECURRING
                                            }
                                        }
                                    },
                                    enabled = scheduleControlsEnabled,
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(stringResource(R.string.schedule_recurring))
                                        }
                                    }
                                )
                            }
                            if (scheduleType == ScheduleType.RECURRING) {
                                val recurringError = recurringMonths.toIntOrNull()?.let { it < 0 } ?: true
                                OutlinedTextField(
                                    value = recurringMonths,
                                    onValueChange = { recurringMonths = it.filter { ch -> ch.isDigit() } },
                                    label = { Text(stringResource(R.string.recurring_months_label)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    isError = recurringError,
                                    supportingText = { Text(stringResource(R.string.recurring_months_helper)) }
                                )
                            }
                        }

                        if (categoriesForType.isNotEmpty()) {
                            Text(stringResource(R.string.category), style = MaterialTheme.typography.titleSmall)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                maxItemsInEachRow = 3
                            ) {
                                orderedCategories.forEach { cat ->
                                    FilterChip(
                                        selected = category.equals(cat, ignoreCase = true),
                                        onClick = { category = cat },
                                        label = { Text(cat) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    )
                                }
                            }
                            if (
                                selectedType == TxType.EXPENSE &&
                                category.isNotBlank() &&
                                categoriesForType.none { it.equals(category, ignoreCase = true) }
                            ) {
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { category = it },
                                    label = { Text(stringResource(R.string.category)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        } else {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text(stringResource(R.string.category)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    } else {
                        val savingsChipColors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                SavingsDirection.ADD to R.string.savings_direction_add,
                                SavingsDirection.WITHDRAW to R.string.savings_direction_withdraw
                            ).forEach { (direction, labelRes) ->
                                FilterChip(
                                    modifier = Modifier.weight(1f),
                                    selected = savingsDirection == direction,
                                    onClick = { savingsDirection = direction },
                                    colors = savingsChipColors,
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(stringResource(labelRes))
                                        }
                                    }
                                )
                            }
                        }

                        if (!savingsEnabled) {
                            Text(
                                text = stringResource(R.string.savings_goal_missing_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (savingsEnabled) {
                            var goalsExpanded by remember { mutableStateOf(false) }
                            val selectedGoalName = savingsGoals.firstOrNull { it.id == selectedGoalId }?.name.orEmpty()
                            ExposedDropdownMenuBox(
                                expanded = goalsExpanded,
                                onExpandedChange = { goalsExpanded = !goalsExpanded }
                            ) {
                                OutlinedTextField(
                                    value = if (selectedGoalName.isBlank()) {
                                        stringResource(R.string.savings_goal_picker_placeholder)
                                    } else {
                                        selectedGoalName
                                    },
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.savings_goal_picker_label)) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(goalsExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = goalsExpanded,
                                    onDismissRequest = { goalsExpanded = false }
                                ) {
                                    savingsGoals.forEach { goal ->
                                        DropdownMenuItem(
                                            text = { Text(goal.name) },
                                            onClick = {
                                                selectedGoalId = goal.id
                                                goalsExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                        }
                    }

                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isSavingsTab && selectedType == TxType.EXPENSE) {
                    Button(
                        onClick = { handleTransactionSave(TxType.EXPENSE) },
                        enabled = formValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_expense_transaction))
                    }
                }
                if (!isSavingsTab && selectedType == TxType.INCOME) {
                    Button(
                        onClick = { handleTransactionSave(TxType.INCOME) },
                        enabled = formValid,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_income_transaction))
                    }
                }
                if (isSavingsTab) {
                    Button(
                        onClick = { handleSavingsEntrySave() },
                        enabled = canSaveSavingsEntry,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save_savings_entry))
                    }
                }
            }
        }
    }
}

private fun buildRecurringTransactions(
    base: Transaction,
    repeatCount: Int,
    impactPerOccurrence: Double
): List<Transaction> {
    val safeCount = repeatCount.coerceAtLeast(0)
    val now = System.currentTimeMillis()
    return (0..safeCount).map { offset ->
        val date = addMonthsToDate(base.dateUtcMillis, offset)
        val transactionId = if (offset == 0) base.id else newTxId()
        base.copy(
            id = transactionId,
            dateUtcMillis = date,
            monthKey = monthKey(date),
            planned = true,
            updatedAtLocal = now,
            savingsImpact = if (base.savingsGoalId != null) impactPerOccurrence else 0.0
        )
    }
}

private fun addMonthsToDate(dateMillis: Long, offset: Int): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = dateMillis
        add(Calendar.MONTH, offset)
    }
    return cal.timeInMillis
}
