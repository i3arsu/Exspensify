package com.example.exspensify.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.example.exspensify.core.navigation.BottomNavigationBar
import com.example.exspensify.core.navigation.NavigationGraph

@Composable
fun MainScreen (){
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {BottomNavigationBar(navController)}
    ) { paddingValues ->
        NavigationGraph(navController = navController, paddingValues = paddingValues)
    }
}