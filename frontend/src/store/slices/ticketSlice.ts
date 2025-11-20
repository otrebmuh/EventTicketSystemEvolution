import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { ticketService, Ticket } from '../../services/ticketService';
import { ApiException } from '../../services/api';

interface TicketState {
  tickets: Ticket[];
  currentTicket: Ticket | null;
  loading: boolean;
  error: string | null;
}

const initialState: TicketState = {
  tickets: [],
  currentTicket: null,
  loading: false,
  error: null,
};

// Async thunks
export const fetchUserTickets = createAsyncThunk(
  'ticket/fetchUserTickets',
  async (userId: string, { rejectWithValue }) => {
    try {
      const tickets = await ticketService.getTicketsByUserId(userId);
      return tickets;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to fetch tickets. Please try again.');
    }
  }
);

export const fetchTicketsByOrderId = createAsyncThunk(
  'ticket/fetchTicketsByOrderId',
  async (orderId: string, { rejectWithValue }) => {
    try {
      const tickets = await ticketService.getTicketsByOrderId(orderId);
      return tickets;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to fetch tickets. Please try again.');
    }
  }
);

export const fetchTicketById = createAsyncThunk(
  'ticket/fetchTicketById',
  async (ticketId: string, { rejectWithValue }) => {
    try {
      const ticket = await ticketService.getTicketById(ticketId);
      return ticket;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to fetch ticket. Please try again.');
    }
  }
);

export const cancelTicket = createAsyncThunk(
  'ticket/cancelTicket',
  async (ticketId: string, { rejectWithValue }) => {
    try {
      await ticketService.cancelTicket(ticketId);
      return ticketId;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to cancel ticket. Please try again.');
    }
  }
);

const ticketSlice = createSlice({
  name: 'ticket',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    clearCurrentTicket: (state) => {
      state.currentTicket = null;
    },
  },
  extraReducers: (builder) => {
    // Fetch user tickets
    builder
      .addCase(fetchUserTickets.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserTickets.fulfilled, (state, action) => {
        state.loading = false;
        state.tickets = action.payload;
      })
      .addCase(fetchUserTickets.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Fetch tickets by order ID
    builder
      .addCase(fetchTicketsByOrderId.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTicketsByOrderId.fulfilled, (state, action) => {
        state.loading = false;
        state.tickets = action.payload;
      })
      .addCase(fetchTicketsByOrderId.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Fetch ticket by ID
    builder
      .addCase(fetchTicketById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchTicketById.fulfilled, (state, action) => {
        state.loading = false;
        state.currentTicket = action.payload;
      })
      .addCase(fetchTicketById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Cancel ticket
    builder
      .addCase(cancelTicket.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(cancelTicket.fulfilled, (state, action) => {
        state.loading = false;
        // Update ticket status in the list
        const ticketIndex = state.tickets.findIndex(t => t.id === action.payload);
        if (ticketIndex !== -1) {
          state.tickets[ticketIndex].status = 'CANCELLED' as any;
        }
        if (state.currentTicket?.id === action.payload) {
          state.currentTicket.status = 'CANCELLED' as any;
        }
      })
      .addCase(cancelTicket.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearError, clearCurrentTicket } = ticketSlice.actions;
export default ticketSlice.reducer;
