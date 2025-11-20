# Event Ticket Booking System - Frontend

React-based frontend application for the Event Ticket Booking System.

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **React Router** - Client-side routing
- **Redux Toolkit** - State management
- **Tailwind CSS** - Styling framework
- **Vite** - Build tool and dev server

## Project Structure

```
src/
├── components/          # Reusable UI components
│   └── Layout/         # Layout components (Header, Footer)
├── pages/              # Page components
├── store/              # Redux store configuration
│   ├── slices/        # Redux slices
│   ├── hooks.ts       # Typed Redux hooks
│   └── store.ts       # Store configuration
├── App.tsx            # Main app component with routing
├── main.tsx           # Application entry point
└── index.css          # Global styles with Tailwind
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
npm install
```

### Development

Start the development server:

```bash
npm run dev
```

The application will be available at `http://localhost:3000`

### Build

Build for production:

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

## Features

- **Routing**: Multi-page application with React Router
- **State Management**: Centralized state with Redux Toolkit
- **Responsive Design**: Mobile-first design with Tailwind CSS
- **Type Safety**: Full TypeScript support
- **Authentication**: Login/Register pages with state management
- **Event Browsing**: Search and filter events
- **User Dashboard**: View tickets and order history

## API Integration

The frontend is configured to proxy API requests to the backend services:

- API Base URL: `http://localhost:8080/api`
- Proxy configured in `vite.config.ts`

## Available Routes

- `/` - Home page
- `/login` - User login
- `/register` - User registration
- `/events` - Browse events
- `/events/:id` - Event details
- `/dashboard` - User dashboard
- `*` - 404 Not Found page

## Redux Store Structure

### Auth Slice
- User authentication state
- JWT token management
- Login/logout actions

### Event Slice
- Events list
- Selected event
- Search query and filters
- Loading and error states

## Next Steps

This is the initial project structure. Future tasks will implement:

1. Authentication components with API integration
2. Event browsing with real data
3. Ticket purchase flow
4. User dashboard with order history
5. Ticket management features
