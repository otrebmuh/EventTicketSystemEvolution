import { configureStore } from '@reduxjs/toolkit'
import authReducer from './slices/authSlice'
import eventReducer from './slices/eventSlice'
import orderReducer from './slices/orderSlice'
import ticketReducer from './slices/ticketSlice'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    event: eventReducer,
    order: orderReducer,
    ticket: ticketReducer,
  },
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
