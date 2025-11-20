import { createSlice, PayloadAction } from '@reduxjs/toolkit'

interface Event {
  id: string
  name: string
  description: string
  eventDate: string
  venueName: string
  venueAddress: string
  category: string
  imageUrl?: string
  status: string
}

interface EventState {
  events: Event[]
  selectedEvent: Event | null
  loading: boolean
  error: string | null
  searchQuery: string
  filters: {
    category?: string
    dateRange?: { start: string; end: string }
    city?: string
  }
}

const initialState: EventState = {
  events: [],
  selectedEvent: null,
  loading: false,
  error: null,
  searchQuery: '',
  filters: {},
}

const eventSlice = createSlice({
  name: 'event',
  initialState,
  reducers: {
    fetchEventsStart: (state) => {
      state.loading = true
      state.error = null
    },
    fetchEventsSuccess: (state, action: PayloadAction<Event[]>) => {
      state.loading = false
      state.events = action.payload
    },
    fetchEventsFailure: (state, action: PayloadAction<string>) => {
      state.loading = false
      state.error = action.payload
    },
    setSelectedEvent: (state, action: PayloadAction<Event | null>) => {
      state.selectedEvent = action.payload
    },
    setSearchQuery: (state, action: PayloadAction<string>) => {
      state.searchQuery = action.payload
    },
    setFilters: (state, action: PayloadAction<EventState['filters']>) => {
      state.filters = action.payload
    },
    clearError: (state) => {
      state.error = null
    },
  },
})

export const {
  fetchEventsStart,
  fetchEventsSuccess,
  fetchEventsFailure,
  setSelectedEvent,
  setSearchQuery,
  setFilters,
  clearError,
} = eventSlice.actions
export default eventSlice.reducer
