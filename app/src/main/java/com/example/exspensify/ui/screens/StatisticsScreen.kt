package com.example.exspensify.ui.screens

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.exspensify.core.util.parseColor
import com.example.exspensify.domain.model.*
import com.example.exspensify.ui.statistics.StatisticsEvent
import com.example.exspensify.ui.statistics.StatisticsViewModel
import java.time.LocalDateTime

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filters
                item {
                    FilterSection(
                        filter = uiState.filter,
                        onMonthChanged = { viewModel.onEvent(StatisticsEvent.MonthChanged(it)) },
                        onYearChanged = { viewModel.onEvent(StatisticsEvent.YearChanged(it)) },
                        onTypeChanged = { viewModel.onEvent(StatisticsEvent.TypeChanged(it)) }
                    )
                }

                // Total Summary with Insights
                item {
                    TotalSummaryCard(
                        total = uiState.totalAmount,
                        type = uiState.filter.type
                    )
                }

                // Quick Insights
                if (uiState.categoryStatistics.isNotEmpty()) {
                    item {
                        QuickInsightsSection(
                            categories = uiState.categoryStatistics
                        )
                    }
                }

                // Pie Chart - Category Distribution
                if (uiState.categoryStatistics.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Category Distribution",
                            icon = Icons.Default.PieChart
                        )
                    }

                    item {
                        PieChartCard(categories = uiState.categoryStatistics)
                    }

                    items(uiState.categoryStatistics) { category ->
                        CategoryLegendItem(category)
                    }
                }

                // Bar Chart - Daily Breakdown
                if (uiState.dailyStatistics.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Daily Breakdown",
                            subtitle = getMonthName(uiState.filter.month),
                            icon = Icons.Default.BarChart
                        )
                    }

                    item {
                        BarChartCard(
                            dailyStats = uiState.dailyStatistics,
                            type = uiState.filter.type
                        )
                    }
                }

                // Line Chart - Monthly Trend
                if (uiState.monthlyStatistics.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Monthly Trend",
                            subtitle = "${uiState.filter.year}",
                            icon = Icons.AutoMirrored.Filled.ShowChart
                        )
                    }

                    item {
                        LineChartCard(
                            monthlyStats = uiState.monthlyStatistics,
                            type = uiState.filter.type
                        )
                    }
                }

                // Empty state
                if (uiState.categoryStatistics.isEmpty() &&
                    uiState.dailyStatistics.isEmpty() &&
                    uiState.monthlyStatistics.isEmpty()) {
                    item {
                        EmptyStatisticsState()
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun QuickInsightsSection(
    categories: List<CategoryStatistic>
) {
    val topCategory = categories.firstOrNull()
    val categoryCount = categories.size
    val avgSpending = if (categories.isNotEmpty()) {
        categories.sumOf { it.amount } / categories.size
    } else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Quick Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InsightItem(
                    icon = Icons.Default.Category,
                    value = "$categoryCount",
                    label = "Categories",
                    modifier = Modifier.weight(1f)
                )

                InsightItem(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    value = topCategory?.categoryIcon ?: "—",
                    label = "Top Category",
                    subtitle = topCategory?.categoryName,
                    modifier = Modifier.weight(1f)
                )

                InsightItem(
                    icon = Icons.Default.Calculate,
                    value = "$${String.format("%.0f", avgSpending)}",
                    label = "Avg/Category",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InsightItem(
    icon: ImageVector,
    value: String,
    label: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FilterSection(
    filter: StatisticsFilter,
    onMonthChanged: (Int) -> Unit,
    onYearChanged: (Int) -> Unit,
    onTypeChanged: (StatisticsType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Type Filter
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter.type == StatisticsType.EXPENSE,
                    onClick = { onTypeChanged(StatisticsType.EXPENSE) },
                    label = { Text("Expenses") },
                    leadingIcon = if (filter.type == StatisticsType.EXPENSE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filter.type == StatisticsType.INCOME,
                    onClick = { onTypeChanged(StatisticsType.INCOME) },
                    label = { Text("Income") },
                    leadingIcon = if (filter.type == StatisticsType.INCOME) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = filter.type == StatisticsType.BOTH,
                    onClick = { onTypeChanged(StatisticsType.BOTH) },
                    label = { Text("Both") },
                    leadingIcon = if (filter.type == StatisticsType.BOTH) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }

            // Month & Year
            Text(
                text = "Period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthSelector(
                    selectedMonth = filter.month,
                    onMonthSelected = onMonthChanged,
                    modifier = Modifier.weight(1f)
                )

                YearSelector(
                    selectedYear = filter.year,
                    onYearSelected = onYearChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(
    selectedMonth: Int,
    onMonthSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = months[selectedMonth - 1],
            onValueChange = {},
            readOnly = true,
            label = { Text("Month") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            months.forEachIndexed { index, month ->
                DropdownMenuItem(
                    text = { Text(month) },
                    onClick = {
                        onMonthSelected(index + 1)
                        expanded = false
                    }
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearSelector(
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currentYear = LocalDateTime.now().year
    val years = (currentYear - 5..currentYear).toList().reversed()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedYear.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Year") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year.toString()) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TotalSummaryCard(
    total: Double,
    type: StatisticsType
) {
    val animatedTotal = remember { Animatable(0f) }

    LaunchedEffect(total) {
        animatedTotal.animateTo(
            targetValue = total.toFloat(),
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (type) {
                StatisticsType.EXPENSE -> MaterialTheme.colorScheme.errorContainer
                StatisticsType.INCOME -> MaterialTheme.colorScheme.primaryContainer
                StatisticsType.BOTH -> MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = when (type) {
                    StatisticsType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                    StatisticsType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                    StatisticsType.BOTH -> Icons.Default.AccountBalance
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when (type) {
                    StatisticsType.EXPENSE -> MaterialTheme.colorScheme.error
                    StatisticsType.INCOME -> MaterialTheme.colorScheme.primary
                    StatisticsType.BOTH -> MaterialTheme.colorScheme.secondary
                }
            )

            Text(
                text = when (type) {
                    StatisticsType.EXPENSE -> "Total Expenses"
                    StatisticsType.INCOME -> "Total Income"
                    StatisticsType.BOTH -> "Total Amount"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$${"%.2f".format(animatedTotal.value)}",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PieChartCard(categories: List<CategoryStatistic>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(categories = categories)
        }
    }
}

@Composable
fun PieChart(categories: List<CategoryStatistic>) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(categories) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Canvas(
        modifier = Modifier.size(250.dp)
    ) {
        val canvasSize = size.minDimension
        val radius = canvasSize / 2f
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        var startAngle = -90f

        categories.forEach { category ->
            val sweepAngle = (category.percentage / 100f) * 360f * animatedProgress.value
            val color = parseColor(category.categoryColor.toString())  // Fixed: removed "as String"

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )

            startAngle += sweepAngle
        }

        // Draw white circle in center for donut effect
        drawCircle(
            color = Color.White,
            radius = radius * 0.5f,
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun CategoryLegendItem(category: CategoryStatistic) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(parseColor(category.categoryColor.toString()))  // Fixed: removed "as String"
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(parseColor(category.categoryColor.toString()).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.categoryIcon,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = category.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%.2f".format(category.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${category.percentage.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun BarChartCard(
    dailyStats: List<DailyStatistic>,
    type: StatisticsType
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            if (type == StatisticsType.BOTH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFEF5350), CircleShape)
                        )
                        Text("Expenses", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF66BB6A), CircleShape)
                        )
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                BarChart(dailyStats = dailyStats)
            }
        }
    }
}

@Composable
fun BarChart(dailyStats: List<DailyStatistic>) {
    val maxAmount = dailyStats.maxOfOrNull {
        maxOf(it.expenseAmount, it.incomeAmount)
    } ?: 1.0

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(dailyStats) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (dailyStats.size * 1.5f)
        val chartHeight = size.height * 0.8f
        val bottomY = size.height

        dailyStats.forEachIndexed { index, stat ->
            val x = (index + 0.5f) * (size.width / dailyStats.size)

            val expenseHeight = (stat.expenseAmount / maxAmount) * chartHeight * animatedProgress.value
            val incomeHeight = (stat.incomeAmount / maxAmount) * chartHeight * animatedProgress.value

            // Draw expense bar
            if (stat.expenseAmount > 0) {
                drawRect(
                    color = Color(0xFFEF5350),
                    topLeft = Offset(x - barWidth / 2, bottomY - expenseHeight.toFloat()),
                    size = Size(barWidth, expenseHeight.toFloat())
                )
            }

            // Draw income bar (offset to the right)
            if (stat.incomeAmount > 0) {
                drawRect(
                    color = Color(0xFF66BB6A),
                    topLeft = Offset(x + barWidth / 2, bottomY - incomeHeight.toFloat()),
                    size = Size(barWidth, incomeHeight.toFloat())
                )
            }

            // Draw day label (show every 5 days for readability)
            if (index % 5 == 0 || index == dailyStats.lastIndex) {
                drawContext.canvas.nativeCanvas.drawText(
                    stat.day.toString(),
                    x,
                    bottomY + 20f,
                    Paint().apply {
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun LineChartCard(
    monthlyStats: List<MonthlyStatistic>,
    type: StatisticsType
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Legend
            if (type == StatisticsType.BOTH) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFEF5350), CircleShape)
                        )
                        Text("Expenses", style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF66BB6A), CircleShape)
                        )
                        Text("Income", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                LineChart(monthlyStats = monthlyStats)
            }
        }
    }
}

@Composable
fun LineChart(monthlyStats: List<MonthlyStatistic>) {
    val maxAmount = monthlyStats.maxOfOrNull {
        maxOf(it.expenseAmount, it.incomeAmount)
    } ?: 1.0

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(monthlyStats) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val stepX = size.width / (monthlyStats.size - 1).coerceAtLeast(1)
        val chartHeight = size.height * 0.8f

        // Draw expense line
        for (i in 0 until monthlyStats.size - 1) {
            val current = monthlyStats[i]
            val next = monthlyStats[i + 1]

            val x1 = i * stepX
            val y1 = size.height - (current.expenseAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()
            val x2 = (i + 1) * stepX
            val y2 = size.height - (next.expenseAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()

            drawLine(
                color = Color(0xFFEF5350),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw income line
        for (i in 0 until monthlyStats.size - 1) {
            val current = monthlyStats[i]
            val next = monthlyStats[i + 1]

            val x1 = i * stepX
            val y1 = size.height - (current.incomeAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()
            val x2 = (i + 1) * stepX
            val y2 = size.height - (next.incomeAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()

            drawLine(
                color = Color(0xFF66BB6A),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Draw points
        monthlyStats.forEachIndexed { index, stat ->
            val x = index * stepX

            if (stat.expenseAmount > 0) {
                val y = size.height - (stat.expenseAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()
                drawCircle(
                    color = Color(0xFFEF5350),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            if (stat.incomeAmount > 0) {
                val y = size.height - (stat.incomeAmount / maxAmount * chartHeight * animatedProgress.value).toFloat()
                drawCircle(
                    color = Color(0xFF66BB6A),
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        // Draw month labels (show first, middle, last)
        monthlyStats.forEachIndexed { index, stat ->
            if (index == 0 || index == 5 || index == 11) {
                val x = index * stepX
                drawContext.canvas.nativeCanvas.drawText(
                    stat.monthName,
                    x,
                    size.height + 20f,
                    Paint().apply {
                        textSize = 24f
                        textAlign = Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyStatisticsState() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Text(
                text = "No Data Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add transactions to see your spending statistics and insights",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getMonthName(month: Int): String {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    return months.getOrNull(month - 1) ?: "Unknown"
}