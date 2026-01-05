package com.jan.moneybear.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.jan.moneybear.MoneyBearApp
import com.jan.moneybear.R
import com.jan.moneybear.ui.components.MoneyBearTopBarTitle
import com.jan.moneybear.data.local.Transaction
import com.jan.moneybear.data.local.TxType
import com.jan.moneybear.data.store.SavingsGoal
import com.jan.moneybear.data.store.BalanceBaseline
import kotlinx.coroutines.launch
import com.jan.moneybear.domain.CategorySeriesEntry
import com.jan.moneybear.domain.defaultExpenseCategories
import com.jan.moneybear.domain.defaultIncomeCategories
import com.jan.moneybear.domain.TransactionRepository
import com.jan.moneybear.domain.addMonths
import com.jan.moneybear.domain.monthKey
import com.jan.moneybear.domain.monthWindow
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.Date
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
private const val EMOJI_BUDGET = "💸"
private const val EMOJI_SAVINGS = "🐻"
private const val EMOJI_CATEGORY = "🏷️"
private const val EMOJI_BALANCE = "💰"
private const val EMOJI_TRANSACTIONS = "🧾"
private const val EMOJI_HISTORY = "🕒"
private const val EMOJI_DAILY_LIMIT = "🎯"
private const val EMOJI_DAYS_REMAINING = "⏳"
private const val EMOJI_GAMIFICATION = "🏆"
private const val EMOJI_VIEW_DETAILS = "\uD83D\uDD0D"
private const val BALANCE_CHART_FUTURE_MONTHS = 4L
private const val MILLIS_PER_DAY = 86_400_000L
private const val RECENT_TRANSACTIONS_INDEX = 6
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onSettings: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as MoneyBearApp
    val transactionRepository: TransactionRepository = app.transactionRepository
    val settingsStore = app.settingsStore
    val authRepository = app.authRepository
    val coroutineScope = rememberCoroutineScope()
    var actionTarget by remember { mutableStateOf<Transaction?>(null) }
    var showActionSheet by rememberSaveable { mutableStateOf(false) }
    var transactionPendingDelete by remember { mutableStateOf<Transaction?>(null) }

    val currentMonth = remember { monthKey() }
    val previousMonth = remember(currentMonth) { addMonths(currentMonth, -1) }
    val twoMonthsAgo = remember(currentMonth) { addMonths(currentMonth, -2) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val categoryViewportMonths = remember(currentMonth, isLandscape) {
        val past = if (isLandscape) 8 else 3
        monthWindow(currentMonth, past = past, future = 1)
    }
    val categoryQueryMonths = remember(currentMonth) { monthWindow(currentMonth, past = 24, future = 6) }
    var categoryChartType by rememberSaveable { mutableStateOf(TxType.EXPENSE) }
    val currency by settingsStore.currencyCode.collectAsState(initial = "EUR")
    val defaultExpense = defaultExpenseCategories(context)
    val defaultIncome = defaultIncomeCategories(context)
    val expenseCategories by settingsStore.expenseCategories.collectAsState(initial = defaultExpense)
    val incomeCategories by settingsStore.incomeCategories.collectAsState(initial = defaultIncome)
    val budget by settingsStore.budgetMonthly.collectAsState(initial = null)
    val budgetCycleStartDay by settingsStore.budgetCycleStartDay.collectAsState(initial = 1)
    val savingsGoals by settingsStore.savingsGoals.collectAsState(initial = emptyList())
    val savingsBalances by transactionRepository.savingsBalances().collectAsState(initial = emptyMap())
    val zoneId = remember { ZoneId.systemDefault() }
    val budgetCycle = currentBudgetCycle(budgetCycleStartDay, zoneId)
    val cycleStartMillis = remember(budgetCycle) {
        budgetCycle.start.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val cycleEndMillisExclusive = remember(budgetCycle) {
        budgetCycle.endInclusive.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val expenseSum by transactionRepository.sumExpensesBetween(cycleStartMillis, cycleEndMillisExclusive).collectAsState(initial = 0.0)
    val incomeSum by transactionRepository.sumIncomesBetween(cycleStartMillis, cycleEndMillisExclusive).collectAsState(initial = 0.0)
    val cycleInfo = calculateBudgetCycleInfo(budgetCycle, zoneId)
    val transactionsCurrent by transactionRepository.listMonth(currentMonth).collectAsState(initial = emptyList())
    val transactionsPrevious by transactionRepository.listMonth(previousMonth).collectAsState(initial = emptyList())
    val transactionsTwoMonthsAgo by transactionRepository.listMonth(twoMonthsAgo).collectAsState(initial = emptyList())
    val recentTransactions by transactionRepository.listRecent(5).collectAsState(initial = emptyList())
    val nowMillis = remember { System.currentTimeMillis() }
    val futureTransactions by transactionRepository.listFuture(nowMillis).collectAsState(initial = emptyList())
    val olderTransactions by transactionRepository.listOlderThan(twoMonthsAgo).collectAsState(initial = emptyList())
    val balanceBaselineState = settingsStore.balanceBaseline.collectAsState(initial = null)
    val balanceBaseline = balanceBaselineState.value

    val expenseCategoryEntries by transactionRepository
        .expenseCategorySeries(categoryQueryMonths)
        .collectAsState(initial = emptyList())
    val incomeCategoryEntries by transactionRepository
        .incomeCategorySeries(categoryQueryMonths)
        .collectAsState(initial = emptyList())
    val activeCategoryEntries = if (categoryChartType == TxType.EXPENSE) {
        expenseCategoryEntries
    } else {
        incomeCategoryEntries
    }
    val activeCategories = if (categoryChartType == TxType.EXPENSE) {
        expenseCategories
    } else {
        incomeCategories
    }

    val categoryMonths = remember(activeCategoryEntries, categoryViewportMonths, categoryQueryMonths) {
        val monthsWithData = activeCategoryEntries.map { it.month }.toSet()
        val monthsToShow = categoryQueryMonths.filter { month ->
            month in monthsWithData || month in categoryViewportMonths
        }
        if (monthsToShow.isEmpty()) categoryViewportMonths else monthsToShow
    }

    val balanceChartEndMillis = remember(zoneId, balanceBaseline) {
        val anchor = LocalDate.now(zoneId).plusMonths(BALANCE_CHART_FUTURE_MONTHS)
        anchor.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }
    val balanceTransactionsFlow = remember(balanceBaseline, balanceChartEndMillis) {
        balanceBaseline?.let { baseline ->
            transactionRepository.transactionsBetween(baseline.dateMillis, balanceChartEndMillis)
        }
    }
    val balanceTransactionsState = balanceTransactionsFlow?.collectAsState(initial = emptyList())
    val balanceTransactions = balanceTransactionsState?.value ?: emptyList()
    val balancePoints = remember(balanceBaseline, balanceTransactions, balanceChartEndMillis) {
        if (balanceBaseline != null) {
            buildBalancePoints(baseline = balanceBaseline, transactions = balanceTransactions, endMillis = balanceChartEndMillis)
        } else {
            emptyList()
        }
    }

    val stackedSeries = remember(activeCategoryEntries, categoryMonths, activeCategories) {
        buildStackedCategorySeries(categoryMonths, activeCategoryEntries, activeCategories)
    }
    val stackedCategories = remember(stackedSeries) { stackedSeries.keys.sortedByDescending { category -> stackedSeries[category]?.sumOf { it } ?: 0.0 } }
    val categoryColors = remember(stackedCategories) {
        stackedCategories.associateWith { categoryColor(it) }
    }

    val categoryTotalsPerMonth = remember(stackedSeries) {
        categoryMonths.indices.map { monthIndex ->
            stackedCategories.sumOf { category -> stackedSeries[category]?.getOrNull(monthIndex) ?: 0.0 }
        }
    }
    val categoryTitle = if (categoryChartType == TxType.EXPENSE) {
        stringResource(R.string.chart_expense_categories_title)
    } else {
        stringResource(R.string.chart_income_categories_title)
    }
    val categoryToggleLabel = if (categoryChartType == TxType.EXPENSE) {
        stringResource(R.string.chart_show_income)
    } else {
        stringResource(R.string.chart_show_expenses)
    }

    val remainingBudget = budget?.minus(expenseSum)
    val utilization = budget?.takeIf { it > 0.0 }?.let { (expenseSum / it).coerceIn(0.0, 1.0) }
    val dailyLimit = if (remainingBudget != null && remainingBudget > 0 && cycleInfo.daysRemaining > 0) {
        remainingBudget / cycleInfo.daysRemaining
    } else null

    var showCategoryDialog by rememberSaveable { mutableStateOf(false) }
    var showBalanceDialog by rememberSaveable { mutableStateOf(false) }
    var historyExpanded by rememberSaveable { mutableStateOf(false) }

    val user = authRepository.currentUser
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { MoneyBearTopBarTitle() },
                navigationIcon = {
                    val photoUrl = user?.photoUrl
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = stringResource(R.string.user_avatar_cd),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.ic_launcher_foreground),
                            error = painterResource(R.drawable.ic_launcher_foreground)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = stringResource(R.string.user_avatar_cd),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(40.dp)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                expanded = fabExpanded,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_transaction)
                    )
                },
                text = {
                    Text(text = stringResource(R.string.add_transaction_extended))
                }
            )
        }
    ) { paddingValues ->
        val density = LocalDensity.current
        val contentTopPaddingPx = with(density) {
            (paddingValues.calculateTopPadding() + 20.dp).toPx()
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                BudgetSummaryCard(
                    budget = budget,
                    expense = expenseSum,
                    remaining = remainingBudget,
                    utilization = utilization,
                    currency = currency
                )
            }
            item {
                KpiRow(
                    dailyLimit = dailyLimit,
                    currency = currency,
                    daysRemaining = cycleInfo.daysRemaining,
                    totalDays = cycleInfo.totalDays
                )
            }
            item {
                SavingsProgressCard(
                    goals = savingsGoals,
                    balances = savingsBalances,
                    currency = currency
                )
            }
            item {
                CategoryChartCard(
                    months = categoryMonths,
                    viewportMonths = categoryViewportMonths,
                    categories = stackedCategories,
                    values = stackedSeries,
                    totals = categoryTotalsPerMonth,
                    colors = categoryColors,
                    title = categoryTitle,
                    toggleLabel = categoryToggleLabel,
                    onToggle = {
                        categoryChartType = if (categoryChartType == TxType.EXPENSE) {
                            TxType.INCOME
                        } else {
                            TxType.EXPENSE
                        }
                    },
                    onExpand = { showCategoryDialog = true }
                )
            }
            item {
                BalanceChartCard(
                    baseline = balanceBaseline,
                    points = balancePoints,
                    currency = currency,
                    onOpenSettings = onSettings,
                    onExpand = { showBalanceDialog = true }
                )
            }
            item {
                GamificationCard(
                    budget = budget,
                    expense = expenseSum,
                    income = incomeSum,
                    savingsGoals = savingsGoals,
                    savingsBalances = savingsBalances,
                    futureCount = futureTransactions.size
                )
            }
            item {
                RecentTransactionsCard(
                    recentTransactions = recentTransactions,
                    currency = currency,
                    historyExpanded = historyExpanded,
                    onHistoryToggle = { expanded ->
                        historyExpanded = expanded
                        if (expanded) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(RECENT_TRANSACTIONS_INDEX)
                            }
                        }
                    },
                    future = futureTransactions,
                    current = transactionsCurrent,
                    previous = transactionsPrevious,
                    twoAgo = transactionsTwoMonthsAgo,
                    olderLocal = olderTransactions,
                    onAccordionExpanded = { headerY ->
                        coroutineScope.launch {
                            val delta = headerY - contentTopPaddingPx
                            if (delta > 0f) {
                                listState.animateScrollBy(delta)
                            }
                        }
                    },
                    onTransactionClick = { transaction ->
                        actionTarget = transaction
                        showActionSheet = true
                    }
                )
            }
        }
    }

    if (showCategoryDialog) {
        CategoryChartDialog(
            onDismiss = { showCategoryDialog = false },
            title = categoryTitle,
            months = categoryMonths,
            viewportMonths = categoryViewportMonths,
            categories = stackedCategories,
            values = stackedSeries,
            colors = categoryColors,
            totals = categoryTotalsPerMonth,
            currency = currency
        )
    }

    if (showBalanceDialog) {
        BalanceChartDialog(
            baseline = balanceBaseline,
            points = balancePoints,
            currency = currency,
            onDismiss = { showBalanceDialog = false },
            onOpenSettings = onSettings
        )
    }

    if (showActionSheet) {
        actionTarget?.let { target ->
            TransactionActionDialog(
                transaction = target,
                onDismiss = {
                    showActionSheet = false
                    actionTarget = null
                },
                onEdit = { tx ->
                    showActionSheet = false
                    actionTarget = null
                    onEditTransaction(tx.id)
                },
                onDelete = { tx ->
                    showActionSheet = false
                    actionTarget = null
                    transactionPendingDelete = tx
                }
            )
        }
    }

    transactionPendingDelete?.let { target ->
        DeleteTransactionDialog(
            transaction = target,
            onConfirm = {
                coroutineScope.launch {
                    transactionRepository.delete(target.id)
                }
                transactionPendingDelete = null
            },
            onDismiss = { transactionPendingDelete = null }
        )
    }

}
@Composable
private fun CategoryChartCard(
    months: List<String>,
    viewportMonths: List<String>,
    categories: List<String>,
    values: Map<String, List<Double>>,
    totals: List<Double>,
    colors: Map<String, Color>,
    title: String,
    toggleLabel: String,
    onToggle: () -> Unit,
    onExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val titleIconSlot = 18.dp
            val titleIconSpacing = 4.dp
            val titleLinkSpacing = 0.dp
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.width(titleIconSlot),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = EMOJI_CATEGORY,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.width(titleIconSpacing))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onExpand) {
                        Text(text = EMOJI_VIEW_DETAILS, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(modifier = Modifier.height(titleLinkSpacing))
                Text(
                    text = toggleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = titleIconSlot + titleIconSpacing)
                        .clickable(onClick = onToggle)
                )
            }
            if (categories.isEmpty() || totals.all { it <= 0.0 }) {
                Text(
                    text = stringResource(R.string.chart_no_data_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                var legendExpanded by rememberSaveable(categories.size) { mutableStateOf(false) }
                val maxLegendItems = 3
                val visibleCategories = if (legendExpanded) categories else categories.take(maxLegendItems)
                StackedBarChart(
                    months = months,
                    categories = categories,
                    values = values,
                    colors = colors,
                    viewportMonths = viewportMonths,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendRow(
                        items = visibleCategories.map { category ->
                            LegendItem(
                                color = colors[category] ?: MaterialTheme.colorScheme.primary,
                                label = category
                            )
                        }
                    )
                    if (categories.size > maxLegendItems) {
                        val remainingCount = categories.size - maxLegendItems
                        TextButton(
                            onClick = { legendExpanded = !legendExpanded },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (legendExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (legendExpanded) {
                                    stringResource(R.string.chart_legend_show_less)
                                } else {
                                    stringResource(R.string.chart_legend_show_more, remainingCount)
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceChartCard(
    baseline: BalanceBaseline?,
    points: List<BalancePoint>,
    currency: String,
    onOpenSettings: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
    showExpandButton: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        BalanceChartContent(
            baseline = baseline,
            points = points,
            currency = currency,
            onOpenSettings = onOpenSettings,
            onExpand = onExpand,
            showExpandButton = showExpandButton
        )
    }
}

@Composable
private fun BalanceChartContent(
    baseline: BalanceBaseline?,
    points: List<BalancePoint>,
    currency: String,
    onOpenSettings: () -> Unit,
    onExpand: () -> Unit,
    showExpandButton: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$EMOJI_BALANCE ${stringResource(R.string.balance_chart_title)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (showExpandButton && baseline != null && points.size >= 2) {
                IconButton(onClick = onExpand) {
                    Text(text = EMOJI_VIEW_DETAILS, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        when {
            baseline == null -> {
                Text(
                    text = stringResource(R.string.balance_chart_setup_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.open_settings))
                }
            }
            points.size < 2 -> {
                Text(
                    text = stringResource(R.string.balance_chart_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                val nowMillis = System.currentTimeMillis()
                val currentPoint = points.lastOrNull { it.timeMillis <= nowMillis } ?: points.first()
                BalanceSummaryRow(
                    total = currentPoint.total,
                    reserved = currentPoint.reserved,
                    available = currentPoint.available,
                    currency = currency
                )
                BalanceStackedAreaChart(points = points)
                BalanceLegend(
                    availableLabel = stringResource(R.string.balance_chart_available_label),
                    reservedLabel = stringResource(R.string.balance_chart_reserved_label)
                )
            }
        }
    }
}

@Composable
private fun BalanceSummaryRow(total: Double, reserved: Double, available: Double, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryValueColumn(
            title = stringResource(R.string.balance_chart_total_label),
            value = total,
            currency = currency,
            modifier = Modifier.weight(1f)
        )
        SummaryValueColumn(
            title = stringResource(R.string.balance_chart_reserved_label),
            value = reserved,
            currency = currency,
            modifier = Modifier.weight(1f)
        )
        SummaryValueColumn(
            title = stringResource(R.string.balance_chart_available_label),
            value = available,
            currency = currency,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryValueColumn(
    title: String,
    value: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatAmount(value),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = currency,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun BalanceStackedAreaChart(
    points: List<BalancePoint>,
    modifier: Modifier = Modifier
) {
    val reservedColor = MaterialTheme.colorScheme.secondary
    val availableColor = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val scrollState = rememberScrollState()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val visibleDays = if (isLandscape) 180 else 120
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val startMillis = points.first().timeMillis
        val endMillis = points.last().timeMillis
        val rangeMillis = (endMillis - startMillis).coerceAtLeast(1L)
        val rangeDays = (rangeMillis.toDouble() / MILLIS_PER_DAY.toDouble()).coerceAtLeast(1.0)
        val dayWidth = maxWidth / visibleDays.toFloat()
        val computedWidth = dayWidth * rangeDays.toFloat()
        val chartWidth = if (computedWidth < maxWidth) maxWidth else computedWidth
        val viewportWidthPx = with(density) { maxWidth.toPx() }
        val dayWidthPx = with(density) { dayWidth.toPx() }
        val todayMillis = System.currentTimeMillis().coerceIn(startMillis, endMillis)
        val todayOffsetDays = (todayMillis - startMillis).toFloat() / MILLIS_PER_DAY
        val targetScroll = ((todayOffsetDays * dayWidthPx) - (viewportWidthPx / 2f))
            .roundToInt()
            .coerceIn(0, scrollState.maxValue)
        LaunchedEffect(chartWidth, scrollState.maxValue, visibleDays, startMillis, endMillis) {
            if (scrollState.maxValue > 0) {
                scrollState.scrollTo(targetScroll)
            }
        }
        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            Canvas(
                modifier = Modifier
                    .width(chartWidth)
                    .height(220.dp)
            ) {
                val xMin = points.first().timeMillis.toFloat()
                val xMax = points.last().timeMillis.toFloat()
                val xRange = (xMax - xMin).takeIf { it != 0f } ?: 1f
                val minValue = points.minOf { min(it.available, it.total) }.coerceAtMost(0.0)
                val maxValue = points.maxOf { max(it.available, it.total) }.coerceAtLeast(1.0)
                val valueRange = (maxValue - minValue).takeIf { it != 0.0 } ?: 1.0
                fun mapX(time: Long): Float = ((time - xMin) / xRange) * size.width
                fun mapY(value: Double): Float =
                    size.height - (((value - minValue) / valueRange).toFloat() * size.height)

                val baselineY = mapY(minValue)
                val availablePath = Path().apply {
                    moveTo(mapX(points.first().timeMillis), baselineY)
                    points.forEach { point ->
                        lineTo(mapX(point.timeMillis), mapY(point.available))
                    }
                    lineTo(mapX(points.last().timeMillis), baselineY)
                    close()
                }
                val reservedPath = Path().apply {
                    moveTo(mapX(points.first().timeMillis), mapY(points.first().available))
                    points.forEach { point ->
                        lineTo(mapX(point.timeMillis), mapY(point.total))
                    }
                    points.asReversed().forEach { point ->
                        lineTo(mapX(point.timeMillis), mapY(point.available))
                    }
                    close()
                }
                drawPath(
                    path = reservedPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            reservedColor.copy(alpha = 0.8f),
                            reservedColor.copy(alpha = 0.4f)
                        )
                    )
                )
                drawPath(
                    path = availablePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            availableColor.copy(alpha = 0.9f),
                            availableColor.copy(alpha = 0.5f)
                        )
                    )
                )
                val zeroY = mapY(0.0)
                if (zeroY in 0f..size.height) {
                    drawLine(
                        color = outline,
                        start = Offset(0f, zeroY),
                        end = Offset(size.width, zeroY),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }
            val startLabel = dateFormatter.format(startMillis)
            val todayLabel = dateFormatter.format(todayMillis)
            val endLabel = dateFormatter.format(endMillis)
            Row(
                modifier = Modifier.width(chartWidth),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = startLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = todayLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = endLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BalanceLegend(
    availableLabel: String,
    reservedLabel: String
) {
    LegendRow(
        items = listOf(
            LegendItem(MaterialTheme.colorScheme.primary, availableLabel),
            LegendItem(MaterialTheme.colorScheme.secondary, reservedLabel)
        )
    )
}

@Suppress("UnusedBoxWithConstraintsScope")
@Composable
private fun StackedBarChart(
    months: List<String>,
    categories: List<String>,
    values: Map<String, List<Double>>,
    colors: Map<String, Color>,
    viewportMonths: List<String>,
    modifier: Modifier = Modifier
) {
    val totals = remember(months, categories, values) {
        months.indices.map { index ->
            categories.sumOf { category -> values[category]?.getOrNull(index) ?: 0.0 }
        }
    }
    val maxTotal = totals.maxOrNull()?.takeIf { it > 0 } ?: 0.0
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
    val fallbackBarColor = MaterialTheme.colorScheme.primary
    val barSpacing = 12.dp
    val initialIndex = remember(months, viewportMonths) {
        viewportMonths.firstNotNullOfOrNull { month ->
            val index = months.indexOf(month)
            index.takeIf { it >= 0 }
        } ?: 0
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    LaunchedEffect(months, viewportMonths) {
        val targetIndex = viewportMonths.firstNotNullOfOrNull { month ->
            val index = months.indexOf(month)
            index.takeIf { it >= 0 }
        } ?: 0
        if (targetIndex != listState.firstVisibleItemIndex) {
            listState.scrollToItem(targetIndex)
        }
    }
    BoxWithConstraints(modifier = modifier) {
        val targetVisibleBars = viewportMonths.size.coerceAtLeast(1)
        val availableWidth = maxWidth - (barSpacing * (targetVisibleBars - 1).toFloat())
        val barWidth = (availableWidth / targetVisibleBars.toFloat()).coerceAtLeast(24.dp)
        val hasBoundedHeight = constraints.hasBoundedHeight
        val chartHeight = if (hasBoundedHeight) maxHeight else 240.dp
        val labelSpace = 28.dp
        val barAreaHeight = (chartHeight - labelSpace).coerceAtLeast(0.dp)
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(barSpacing),
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) {
            items(months.size) { index ->
                val month = months[index]
                Column(
                    modifier = Modifier
                        .width(barWidth)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(barAreaHeight)
                    ) {
                        val axisStroke = 1.dp.toPx()
                        drawLine(
                            color = outlineVariantColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = axisStroke
                        )
                        if (maxTotal <= 0.0) return@Canvas
                        var topOffset = size.height
                        categories.forEach { category ->
                            val amount = values[category]?.getOrNull(index) ?: 0.0
                            if (amount <= 0.0) return@forEach
                            val normalized = (amount / maxTotal).toFloat().coerceIn(0f, 1f)
                            val height = normalized * (size.height - axisStroke)
                            val top = topOffset - height
                            drawRoundRect(
                                color = colors[category] ?: fallbackBarColor,
                                topLeft = Offset(0f, top),
                                size = Size(size.width, height),
                                cornerRadius = CornerRadius(x = size.width / 4f, y = size.width / 4f)
                            )
                            topOffset = top
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatMonthShort(month),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LegendRow(items: List<LegendItem>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item -> LegendChip(item) }
    }
}

@Composable
private fun LegendChip(item: LegendItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(item.color)
        )
        Text(text = item.label, style = MaterialTheme.typography.bodySmall)
    }
}

private data class LegendItem(val color: Color, val label: String)

private data class Badge(
    val title: String,
    val description: String,
    val achieved: Boolean
)

@Composable
private fun SavingsProgressCard(
    goals: List<SavingsGoal>,
    balances: Map<String, Double>,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "$EMOJI_SAVINGS ${stringResource(R.string.savings_section_title)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (goals.isEmpty()) {
                Text(
                    text = stringResource(R.string.savings_not_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                goals.forEach { goal ->
                    val balance = balances[goal.id] ?: 0.0
                    val ratio = if (goal.target > 0.0) (balance / goal.target).coerceIn(0.0, 1.0) else 0.0
                    val expectedProgress = goal.deadlineMillis?.let { deadlineMillis ->
                        val now = System.currentTimeMillis()
                        val start = goal.updatedAt.takeIf { it > 0L } ?: now
                        when {
                            deadlineMillis <= start -> 1f
                            now <= start -> 0f
                            else -> {
                                val elapsed = now - start
                                val total = deadlineMillis - start
                                (elapsed.toDouble() / total.toDouble()).toFloat().coerceIn(0f, 1f)
                            }
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Progress baar lineaarse eesmärgi punase kriipsuga
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { ratio.toFloat() },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            
                            val indicatorColor = MaterialTheme.colorScheme.error
                            expectedProgress?.let { expected ->
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val xPosition = size.width * expected.coerceIn(0f, 1f)
                                    drawLine(
                                        color = indicatorColor,
                                        start = Offset(xPosition, 0f),
                                        end = Offset(xPosition, size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = stringResource(
                                R.string.savings_goal_progress_label,
                                formatCurrency(balance, currency),
                                formatCurrency(goal.target, currency)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        goal.deadlineMillis?.let { deadlineMillis ->
                            val deadlineLabel = remember(deadlineMillis) { formatGoalDeadline(deadlineMillis) }
                            Text(
                                text = stringResource(R.string.savings_goal_deadline_badge, deadlineLabel),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationCard(
    budget: Double?,
    expense: Double,
    income: Double,
    savingsGoals: List<SavingsGoal>,
    savingsBalances: Map<String, Double>,
    futureCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$EMOJI_GAMIFICATION " + stringResource(R.string.gamification_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val totalTarget = savingsGoals.sumOf { it.target.coerceAtLeast(0.0) }
            val totalSavings = savingsGoals.sumOf { goal -> savingsBalances[goal.id] ?: 0.0 }
            val savingsRatio = if (totalTarget > 0.0) (totalSavings / totalTarget).coerceIn(0.0, 1.0) else 0.0
            val badges = listOf(
                Badge(
                    title = stringResource(R.string.badge_budget_guardian),
                    description = stringResource(R.string.badge_budget_guardian_desc),
                    achieved = budget != null && budget > 0.0 && expense <= budget * 0.9
                ),
                Badge(
                    title = stringResource(R.string.badge_savings_starter),
                    description = stringResource(R.string.badge_savings_starter_desc),
                    achieved = savingsRatio >= 0.25
                ),
                Badge(
                    title = stringResource(R.string.badge_savings_champion),
                    description = stringResource(R.string.badge_savings_champion_desc),
                    achieved = savingsRatio >= 0.75
                ),
                Badge(
                    title = stringResource(R.string.badge_future_planner),
                    description = stringResource(R.string.badge_future_planner_desc),
                    achieved = futureCount > 0
                ),
                Badge(
                    title = stringResource(R.string.badge_net_positive),
                    description = stringResource(R.string.badge_net_positive_desc),
                    achieved = income >= expense
                )
            )
            val unlocked = badges.count { it.achieved }
            Text(
                text = stringResource(R.string.gamification_badges_summary, unlocked, badges.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                badges.forEach { badge ->
                    Surface(
                        color = if (badge.achieved) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (badge.achieved) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                tint = if (badge.achieved) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (badge.achieved) FontWeight.SemiBold else FontWeight.Medium
                                )
                                Text(
                                    text = badge.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChartDialog(
    onDismiss: () -> Unit,
    title: String,
    months: List<String>,
    viewportMonths: List<String>,
    categories: List<String>,
    values: Map<String, List<Double>>,
    colors: Map<String, Color>,
    totals: List<Double>,
    currency: String
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            var selectedTab by rememberSaveable { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
                    }
                }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.chart_tab_chart)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.chart_tab_table)) }
                    )
                }
                if (selectedTab == 0) {
                    StackedBarChart(
                        months = months,
                        categories = categories,
                        values = values,
                        colors = colors,
                        viewportMonths = viewportMonths,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                    )
                } else {
                    CategoryChartTable(
                        months = months,
                        categories = categories,
                        values = values,
                        currency = currency
                    )
                }
            }
        }
    }
}
@Composable
private fun CategoryChartTable(
    months: List<String>,
    categories: List<String>,
    values: Map<String, List<Double>>,
    currency: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(months.indices.toList()) { index ->
            val monthKey = months[index]
            val rows = categories.mapNotNull { category ->
                val amount = values[category]?.getOrNull(index) ?: 0.0
                if (amount > 0) category to amount else null
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = formatMonthLong(monthKey),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.chart_no_data_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    rows.forEach { (category, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = category, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = formatCurrency(amount, currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceChartDialog(
    baseline: BalanceBaseline?,
    points: List<BalancePoint>,
    currency: String,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            var selectedTab by rememberSaveable { mutableStateOf(0) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.balance_chart_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.back))
                    }
                }
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(stringResource(R.string.chart_tab_chart)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(stringResource(R.string.chart_tab_table)) }
                    )
                }
                if (selectedTab == 0) {
                    BalanceChartContent(
                        baseline = baseline,
                        points = points,
                        currency = currency,
                        onOpenSettings = onOpenSettings,
                        onExpand = {},
                        showExpandButton = false
                    )
                } else {
                    BalanceChartTable(
                        baseline = baseline,
                        points = points,
                        currency = currency
                    )
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun BalanceChartTable(
    baseline: BalanceBaseline?,
    points: List<BalancePoint>,
    currency: String
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val rows = remember(points) {
        points.sortedBy { it.timeMillis }
    }
    if (rows.isEmpty()) {
        val message = if (baseline == null) {
            stringResource(R.string.balance_chart_no_data)
        } else {
            stringResource(R.string.chart_no_data_hint)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(rows) { point ->
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatter.format(Date(point.timeMillis)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.balance_chart_total_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = formatCurrency(point.total, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.balance_chart_available_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = formatCurrency(point.available, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.balance_chart_reserved_label), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = formatCurrency(point.reserved, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

private data class BalancePoint(
    val timeMillis: Long,
    val total: Double,
    val reserved: Double
) {
    val available: Double get() = total - reserved
}

private fun buildBalancePoints(
    baseline: BalanceBaseline,
    transactions: List<Transaction>,
    endMillis: Long
): List<BalancePoint> {
    if (baseline.dateMillis >= endMillis) return emptyList()
    val sorted = transactions
        .filter { it.dateUtcMillis >= baseline.dateMillis }
        .sortedBy { it.dateUtcMillis }
    val points = mutableListOf<BalancePoint>()
    var balance = baseline.amount
    var reserved = 0.0
    fun snapshot(time: Long) {
        val safeReserved = reserved.coerceIn(0.0, max(balance, 0.0))
        points += BalancePoint(timeMillis = time, total = balance, reserved = safeReserved)
    }
    snapshot(baseline.dateMillis)
    sorted.forEach { tx ->
        val signed = if (tx.type == TxType.INCOME) tx.amount else -tx.amount
        balance += signed
        reserved += tx.savingsImpact
        snapshot(tx.dateUtcMillis)
    }
    val lastTime = points.lastOrNull()?.timeMillis ?: baseline.dateMillis
    val nowMillis = System.currentTimeMillis().coerceAtMost(endMillis)
    if (nowMillis > lastTime) {
        snapshot(nowMillis)
    }
    val finalTime = points.lastOrNull()?.timeMillis ?: baseline.dateMillis
    if (endMillis > finalTime) {
        snapshot(endMillis)
    }
    return points
        .sortedBy { it.timeMillis }
        .distinctBy { it.timeMillis }
}

@Composable
private fun RecentTransactionsCard(
    recentTransactions: List<Transaction>,
    currency: String,
    historyExpanded: Boolean,
    onHistoryToggle: (Boolean) -> Unit,
    onAccordionExpanded: (Float) -> Unit,
    future: List<Transaction>,
    current: List<Transaction>,
    previous: List<Transaction>,
    twoAgo: List<Transaction>,
    olderLocal: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$EMOJI_TRANSACTIONS ${stringResource(R.string.recent_transactions)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    val historyToggleLabel = if (historyExpanded) {
                        stringResource(R.string.hide_transaction_history)
                    } else {
                        stringResource(R.string.view_transaction_history)
                    }
                    IconButton(
                        onClick = { onHistoryToggle(!historyExpanded) },
                        modifier = Modifier.semantics { contentDescription = historyToggleLabel }
                    ) {
                        Text(text = EMOJI_VIEW_DETAILS, style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (recentTransactions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_transactions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        recentTransactions.forEach { transaction ->
                            TransactionRow(transaction = transaction, currency = currency, onClick = onTransactionClick)
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = historyExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "$EMOJI_HISTORY ${stringResource(R.string.transaction_history_title)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TransactionHistoryContent(
                        modifier = Modifier.fillMaxWidth(),
                        currency = currency,
                        future = future,
                        current = current,
                        previous = previous,
                        twoAgo = twoAgo,
                        olderLocal = olderLocal,
                        onAccordionExpanded = onAccordionExpanded,
                        onTransactionClick = onTransactionClick
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionHistoryContent(
    modifier: Modifier = Modifier,
    currency: String,
    future: List<Transaction>,
    current: List<Transaction>,
    previous: List<Transaction>,
    twoAgo: List<Transaction>,
    olderLocal: List<Transaction>,
    onAccordionExpanded: (Float) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    val futureLabel = stringResource(R.string.future_transactions)
    val currentLabel = stringResource(R.string.this_month_transactions)
    val previousLabel = stringResource(R.string.previous_month_transactions)
    val twoAgoLabel = stringResource(R.string.two_months_ago_transactions)
    val olderLabel = stringResource(R.string.older_transactions)
    val olderList = olderLocal
    val sections = listOf(
        TransactionSection(futureLabel, future),
        TransactionSection(currentLabel, current),
        TransactionSection(previousLabel, previous),
        TransactionSection(twoAgoLabel, twoAgo),
        TransactionSection(
            title = olderLabel,
            transactions = olderList
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        sections.forEach { section ->
            TransactionAccordion(
                section = section,
                currency = currency,
                initiallyExpanded = section.title == currentLabel,
                onAccordionExpanded = onAccordionExpanded,
                onTransactionClick = onTransactionClick
            )
        }
    }
}

private data class TransactionSection(
    val title: String,
    val transactions: List<Transaction>,
    val isLoading: Boolean = false,
    val info: String? = null
)

private data class TransactionCategoryGroup(
    val category: String,
    val transactions: List<Transaction>
)

@Composable
private fun TransactionAccordion(
    section: TransactionSection,
    currency: String,
    initiallyExpanded: Boolean,
    onAccordionExpanded: (Float) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    var expanded by rememberSaveable(section.title) { mutableStateOf(initiallyExpanded) }
    var headerY by remember(section.title) { mutableStateOf<Float?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    headerY = coordinates.positionInRoot().y
                }
                .clickable {
                    val next = !expanded
                    expanded = next
                    if (next) {
                        headerY?.let(onAccordionExpanded)
                    }
                }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (section.isLoading) {
                        stringResource(R.string.loading)
                    } else {
                        stringResource(R.string.transaction_count_label, section.transactions.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            when {
                section.isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                section.transactions.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.no_transactions_section_hint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    val categoryGroups = remember(section.transactions) {
                        section.transactions
                            .groupBy { it.category }
                            .map { (category, transactions) ->
                                TransactionCategoryGroup(
                                    category = category,
                                    transactions = transactions.sortedByDescending { tx ->
                                        tx.updatedAtLocal.takeIf { it > 0 } ?: tx.dateUtcMillis
                                    }
                                )
                            }
                            .sortedByDescending { group ->
                                group.transactions.maxOfOrNull { tx -> tx.updatedAtLocal }
                                    ?: group.transactions.maxOfOrNull { tx -> tx.dateUtcMillis }
                                    ?: Long.MIN_VALUE
                            }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        categoryGroups.forEach { group ->
                            TransactionCategoryAccordion(
                                sectionTitle = section.title,
                                group = group,
                                currency = currency,
                                onAccordionExpanded = onAccordionExpanded,
                                onTransactionClick = onTransactionClick
                            )
                        }
                    }
                }
            }
            section.info?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
@Composable
private fun BudgetSummaryCard(
    budget: Double?,
    expense: Double,
    remaining: Double?,
    utilization: Double?,
    currency: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "$EMOJI_BUDGET ${stringResource(R.string.budget_card_title)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (budget == null || budget <= 0.0) {
                Text(
                    text = stringResource(R.string.no_budget_set),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }
            val remainingValue = remaining ?: 0.0
            val progress = utilization?.toFloat() ?: 0f
            val progressColor = when {
                progress <= 0.6f -> MaterialTheme.colorScheme.secondary
                progress <= 0.9f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat(
                    title = stringResource(R.string.spent_label),
                    value = formatCurrency(expense, currency)
                )
                SummaryStat(
                    title = stringResource(R.string.remaining_label),
                    value = formatCurrency(max(remainingValue, 0.0), currency)
                )
            }
            LinearProgressIndicator(
                progress = { progress },
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Text(
                text = stringResource(R.string.utilization_label) + ": " +
                    String.format(Locale.getDefault(), "%.0f%%", progress * 100),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SummaryStat(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun KpiRow(
    dailyLimit: Double?,
    currency: String,
    daysRemaining: Int,
    totalDays: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$EMOJI_DAILY_LIMIT " + stringResource(R.string.daily_limit_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val limitText = dailyLimit?.let { formatDailyLimit(it) } ?: "-"
                val unitText = dailyLimit?.let { stringResource(R.string.per_day_unit, currency) }
                val limitStyle = when {
                    limitText.length > 10 -> MaterialTheme.typography.titleMedium
                    limitText.length > 6 -> MaterialTheme.typography.titleLarge
                    else -> MaterialTheme.typography.headlineSmall
                }
                Text(
                    text = limitText,
                    style = limitStyle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                unitText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$EMOJI_DAYS_REMAINING " + stringResource(R.string.days_remaining_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = daysRemaining.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.days_remaining_unit),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                val progress = if (totalDays > 0) {
                    (totalDays - daysRemaining).coerceIn(0, totalDays) / totalDays.toFloat()
                } else {
                    0f
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun TransactionCategoryAccordion(
    sectionTitle: String,
    group: TransactionCategoryGroup,
    currency: String,
    onAccordionExpanded: (Float) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    var expanded by rememberSaveable(sectionTitle, group.category) { mutableStateOf(false) }
    var headerY by remember(sectionTitle, group.category) { mutableStateOf<Float?>(null) }
    val totalAmount = remember(group.transactions) {
        group.transactions.sumOf { transaction ->
            if (transaction.type == TxType.INCOME) transaction.amount else -transaction.amount
        }
    }
    val formattedTotal = remember(totalAmount, currency) {
        formatCurrency(totalAmount, currency, withSign = true)
    }
    val totalColor = if (totalAmount >= 0) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    headerY = coordinates.positionInRoot().y
                }
                .clickable {
                    val next = !expanded
                    expanded = next
                    if (next) {
                        headerY?.let(onAccordionExpanded)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = group.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.transaction_count_label, group.transactions.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTotal,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = totalColor
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                group.transactions.forEach { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        currency = currency,
                        onClick = onTransactionClick,
                        showCategory = false
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    currency: String,
    onClick: ((Transaction) -> Unit)? = null,
    showCategory: Boolean = true
) {
    val signedAmountValue = if (transaction.type == TxType.INCOME) transaction.amount else -transaction.amount
    val amountText = formatCurrency(signedAmountValue, currency, withSign = true)
    val amountColor = if (signedAmountValue >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val dateText = remember(transaction.dateUtcMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(transaction.dateUtcMillis)
    }
    val shape = RoundedCornerShape(16.dp)
    val clickableModifier = if (onClick != null) {
        Modifier.clickable { onClick(transaction) }
    } else {
        Modifier
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(clickableModifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (showCategory) {
                        Text(
                            transaction.category,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(dateText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium,
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            transaction.note?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (transaction.planned) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.planned_badge),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionActionDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onEdit: (Transaction) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val formattedAmount = formatCurrency(transaction.amount, transaction.currency, withSign = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "$EMOJI_TRANSACTIONS ${stringResource(R.string.transaction_options_title)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                TextButton(onClick = { onEdit(transaction) }) {
                    Text(text = stringResource(R.string.edit_transaction))
                }
                TextButton(onClick = { onDelete(transaction) }) {
                    Text(
                        text = stringResource(R.string.delete_transaction),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun DeleteTransactionDialog(
    transaction: Transaction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val formattedAmount = formatCurrency(transaction.amount, transaction.currency, withSign = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.delete_transaction)) },
        text = {
            Text(
                text = stringResource(
                    R.string.delete_transaction_confirm_details,
                    transaction.category,
                    formattedAmount
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.delete_transaction), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private fun formatCurrency(amount: Double, currency: String, withSign: Boolean = false): String {
    val absolute = abs(amount)
    val base = String.format(Locale.getDefault(), "%.2f %s", absolute, currency)
    if (!withSign) return base
    val sign = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    return if (sign.isEmpty()) base else sign + base
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}

private fun formatDailyLimit(value: Double): String {
    val normalized = value.coerceAtLeast(0.0)
    return String.format(Locale.getDefault(), "%.2f", normalized)
}

private fun formatGoalDeadline(millis: Long): String {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}

private data class BudgetCycle(val start: LocalDate, val endInclusive: LocalDate)

private data class CycleInfo(val totalDays: Int, val daysRemaining: Int)

private fun currentBudgetCycle(startDay: Int, zoneId: ZoneId): BudgetCycle {
    val today = LocalDate.now(zoneId)
    val currentMonth = YearMonth.from(today)
    val normalizedStart = startDay.coerceIn(1, 31)
    fun resolveStartDate(month: YearMonth): LocalDate {
        val day = normalizedStart.coerceAtMost(month.lengthOfMonth())
        return LocalDate.of(month.year, month.month, day)
    }

    val candidateStart = resolveStartDate(currentMonth)
    val cycleStart = if (!today.isBefore(candidateStart)) {
        candidateStart
    } else {
        val previousMonth = currentMonth.minusMonths(1)
        resolveStartDate(previousMonth)
    }

    val nextMonth = YearMonth.from(cycleStart).plusMonths(1)
    val nextStart = resolveStartDate(nextMonth)
    val cycleEnd = nextStart.minusDays(1)
    return BudgetCycle(cycleStart, cycleEnd)
}

private fun calculateBudgetCycleInfo(cycle: BudgetCycle, zoneId: ZoneId): CycleInfo {
    val today = LocalDate.now(zoneId)
    val totalDays = ChronoUnit.DAYS.between(cycle.start, cycle.endInclusive.plusDays(1)).toInt().coerceAtLeast(1)
    val daysRemaining = when {
        today.isAfter(cycle.endInclusive) -> 0
        today.isBefore(cycle.start) -> totalDays
        else -> ChronoUnit.DAYS.between(today, cycle.endInclusive.plusDays(1)).toInt().coerceAtLeast(0)
    }
    return CycleInfo(totalDays, daysRemaining)
}

private fun formatMonthShort(monthKey: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    val yearMonth = YearMonth.parse(monthKey, formatter)
    return yearMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
}

private fun formatMonthLong(monthKey: String): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM", Locale.US)
    val yearMonth = YearMonth.parse(monthKey, formatter)
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$monthName ${yearMonth.year}"
}

private fun categoryColor(category: String): Color {
    val hue = (abs(category.hashCode()) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.55f)
}

private fun buildStackedCategorySeries(
    months: List<String>,
    entries: List<CategorySeriesEntry>,
    baselineCategories: List<String>
): Map<String, List<Double>> {
    val grouped = entries.groupBy { it.category }
    val base = if (grouped.isEmpty()) {
        baselineCategories.associateWith { months.map { 0.0 } }
    } else {
        grouped.mapValues { (_, list) ->
            months.map { month ->
                val amount = list.firstOrNull { it.month == month }?.amount ?: 0.0
                abs(amount)
            }
        }
    }
    return base.filterValues { values -> values.any { it > 0.0 } }
}
