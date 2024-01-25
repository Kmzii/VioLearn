package com.example.mystudytracker.menu

interface DataFetchCallback {
    fun onDataFetched(iWouldLikeItems: List<IWouldLikeItem>)
}
