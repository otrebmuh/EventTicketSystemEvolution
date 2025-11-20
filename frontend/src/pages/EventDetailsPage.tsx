import { useParams } from 'react-router-dom'

const EventDetailsPage = () => {
  const { id } = useParams<{ id: string }>()

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        {/* Event Image */}
        <div className="h-96 bg-gray-200"></div>
        
        {/* Event Details */}
        <div className="p-8">
          <h1 className="text-4xl font-bold mb-4">Event Details</h1>
          <p className="text-gray-600 mb-2">Event ID: {id}</p>
          
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mt-8">
            <div>
              <h2 className="text-2xl font-semibold mb-4">Event Information</h2>
              <div className="space-y-3">
                <p><span className="font-semibold">Date:</span> TBD</p>
                <p><span className="font-semibold">Time:</span> TBD</p>
                <p><span className="font-semibold">Venue:</span> Sample Venue</p>
                <p><span className="font-semibold">Location:</span> Sample City</p>
                <p><span className="font-semibold">Category:</span> Sample Category</p>
              </div>
            </div>
            
            <div>
              <h2 className="text-2xl font-semibold mb-4">Ticket Selection</h2>
              <div className="bg-gray-50 p-6 rounded-lg">
                <p className="text-gray-600 mb-4">Select your tickets:</p>
                <button className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition">
                  Select Tickets
                </button>
              </div>
            </div>
          </div>
          
          <div className="mt-8">
            <h2 className="text-2xl font-semibold mb-4">Description</h2>
            <p className="text-gray-700">
              Event description will be displayed here.
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default EventDetailsPage
