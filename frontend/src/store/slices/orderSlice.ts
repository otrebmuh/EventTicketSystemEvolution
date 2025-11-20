import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { paymentService, Order, TicketPurchaseRequest } from '../../services/paymentService';
import { ApiException } from '../../services/api';

interface OrderState {
  currentOrder: Order | null;
  orders: Order[];
  loading: boolean;
  error: string | null;
  successMessage: string | null;
  pagination: {
    page: number;
    size: number;
    totalPages: number;
    totalElements: number;
  };
}

const initialState: OrderState = {
  currentOrder: null,
  orders: [],
  loading: false,
  error: null,
  successMessage: null,
  pagination: {
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0,
  },
};

// Async thunks
export const purchaseTickets = createAsyncThunk(
  'order/purchaseTickets',
  async (request: TicketPurchaseRequest, { rejectWithValue }) => {
    try {
      const response = await paymentService.purchaseTickets(request);
      // Fetch the complete order details
      const order = await paymentService.getOrderById(response.orderId);
      return { purchaseResponse: response, order };
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Ticket purchase failed. Please try again.');
    }
  }
);

export const fetchUserOrders = createAsyncThunk(
  'order/fetchUserOrders',
  async ({ userId, page = 0, size = 10 }: { userId: string; page?: number; size?: number }, { rejectWithValue }) => {
    try {
      const response = await paymentService.getUserOrders(userId, page, size);
      return response;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to fetch orders. Please try again.');
    }
  }
);

export const fetchOrderById = createAsyncThunk(
  'order/fetchOrderById',
  async (orderId: string, { rejectWithValue }) => {
    try {
      const order = await paymentService.getOrderById(orderId);
      return order;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to fetch order. Please try again.');
    }
  }
);

export const cancelOrder = createAsyncThunk(
  'order/cancelOrder',
  async (orderId: string, { rejectWithValue }) => {
    try {
      const order = await paymentService.cancelOrder(orderId);
      return order;
    } catch (error) {
      if (error instanceof ApiException) {
        return rejectWithValue(error.error.message);
      }
      return rejectWithValue('Failed to cancel order. Please try again.');
    }
  }
);

const orderSlice = createSlice({
  name: 'order',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    clearSuccessMessage: (state) => {
      state.successMessage = null;
    },
    clearCurrentOrder: (state) => {
      state.currentOrder = null;
    },
    setPage: (state, action: PayloadAction<number>) => {
      state.pagination.page = action.payload;
    },
  },
  extraReducers: (builder) => {
    // Purchase tickets
    builder
      .addCase(purchaseTickets.pending, (state) => {
        state.loading = true;
        state.error = null;
        state.successMessage = null;
      })
      .addCase(purchaseTickets.fulfilled, (state, action) => {
        state.loading = false;
        state.currentOrder = action.payload.order;
        state.successMessage = 'Ticket purchase successful!';
      })
      .addCase(purchaseTickets.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Fetch user orders
    builder
      .addCase(fetchUserOrders.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchUserOrders.fulfilled, (state, action) => {
        state.loading = false;
        state.orders = action.payload.content;
        state.pagination.totalPages = action.payload.totalPages;
        state.pagination.totalElements = action.payload.totalElements;
        state.pagination.page = action.payload.number;
        state.pagination.size = action.payload.size;
      })
      .addCase(fetchUserOrders.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Fetch order by ID
    builder
      .addCase(fetchOrderById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchOrderById.fulfilled, (state, action) => {
        state.loading = false;
        state.currentOrder = action.payload;
      })
      .addCase(fetchOrderById.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });

    // Cancel order
    builder
      .addCase(cancelOrder.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(cancelOrder.fulfilled, (state, action) => {
        state.loading = false;
        state.currentOrder = action.payload;
        state.successMessage = 'Order cancelled successfully';
        // Update the order in the orders list if it exists
        const index = state.orders.findIndex(o => o.id === action.payload.id);
        if (index !== -1) {
          state.orders[index] = action.payload;
        }
      })
      .addCase(cancelOrder.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { clearError, clearSuccessMessage, clearCurrentOrder, setPage } = orderSlice.actions;
export default orderSlice.reducer;
