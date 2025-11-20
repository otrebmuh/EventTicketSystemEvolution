import { useEffect, useRef, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../store/hooks';
import { fetchTicketById, cancelTicket, clearCurrentTicket } from '../store/slices/ticketSlice';
import type { RootState } from '../store/store';
import QRCode from 'qrcode';

const TicketDetailsPage = () => {
  const { ticketId } = useParams<{ ticketId: string }>();
  const dispatch = useAppDispatch();
  
  const { currentTicket, loading, error } = useAppSelector((state: RootState) => state.ticket);
  const [qrCodeUrl, setQrCodeUrl] = useState<string>('');
  const [showCancelModal, setShowCancelModal] = useState(false);
  const ticketRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (ticketId) {
      dispatch(fetchTicketById(ticketId));
    }

    return () => {
      dispatch(clearCurrentTicket());
    };
  }, [ticketId, dispatch]);

  useEffect(() => {
    if (currentTicket?.qrCode) {
      // Generate QR code image from the QR code data
      QRCode.toDataURL(currentTicket.qrCode, {
        width: 300,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#FFFFFF',
        },
      })
        .then((url) => {
          setQrCodeUrl(url);
        })
        .catch((err) => {
          console.error('Error generating QR code:', err);
        });
    }
  }, [currentTicket?.qrCode]);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return 'text-green-600 bg-green-50 border-green-200';
      case 'USED':
        return 'text-gray-600 bg-gray-50 border-gray-200';
      case 'CANCELLED':
        return 'text-red-600 bg-red-50 border-red-200';
      case 'EXPIRED':
        return 'text-orange-600 bg-orange-50 border-orange-200';
      default:
        return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const handleDownload = async () => {
    if (!ticketRef.current) return;

    try {
      // Use html2canvas to capture the ticket
      const html2canvas = (await import('html2canvas')).default;
      const canvas = await html2canvas(ticketRef.current, {
        scale: 2,
        backgroundColor: '#ffffff',
      });

      // Convert to blob and download
      canvas.toBlob((blob) => {
        if (blob) {
          const url = URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `ticket-${currentTicket?.ticketNumber}.png`;
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          URL.revokeObjectURL(url);
        }
      });
    } catch (err) {
      console.error('Error downloading ticket:', err);
      alert('Failed to download ticket. Please try again.');
    }
  };

  const handleShare = async () => {
    if (!currentTicket) return;

    const shareData = {
      title: `Ticket for ${currentTicket.eventName}`,
      text: `My ticket for ${currentTicket.eventName} on ${currentTicket.eventDate}`,
      url: window.location.href,
    };

    try {
      if (navigator.share) {
        await navigator.share(shareData);
      } else {
        // Fallback: copy link to clipboard
        await navigator.clipboard.writeText(window.location.href);
        alert('Ticket link copied to clipboard!');
      }
    } catch (err) {
      console.error('Error sharing ticket:', err);
    }
  };

  const handleCancelTicket = async () => {
    if (!ticketId) return;

    try {
      await dispatch(cancelTicket(ticketId)).unwrap();
      setShowCancelModal(false);
      alert('Ticket cancelled successfully');
    } catch (err) {
      console.error('Error cancelling ticket:', err);
    }
  };

  const handlePrint = () => {
    window.print();
  };

  if (loading) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
          <p className="mt-4 text-gray-600">Loading ticket...</p>
        </div>
      </div>
    );
  }

  if (error || !currentTicket) {
    return (
      <div className="container mx-auto px-4 py-8">
        <div className="max-w-2xl mx-auto">
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-8">
            {error || 'Ticket not found'}
          </div>
          <Link to="/dashboard" className="text-blue-600 hover:underline">
            ← Back to Dashboard
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Action Buttons - Hidden on print */}
        <div className="mb-6 flex flex-wrap gap-3 print:hidden">
          <Link
            to="/dashboard"
            className="text-blue-600 hover:underline flex items-center gap-1"
          >
            ← Back to Dashboard
          </Link>
          <div className="flex-1"></div>
          <button
            onClick={handleDownload}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Download
          </button>
          <button
            onClick={handleShare}
            className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.684 13.342C8.886 12.938 9 12.482 9 12c0-.482-.114-.938-.316-1.342m0 2.684a3 3 0 110-2.684m0 2.684l6.632 3.316m-6.632-6l6.632-3.316m0 0a3 3 0 105.367-2.684 3 3 0 00-5.367 2.684zm0 9.316a3 3 0 105.368 2.684 3 3 0 00-5.368-2.684z" />
            </svg>
            Share
          </button>
          <button
            onClick={handlePrint}
            className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 17h2a2 2 0 002-2v-4a2 2 0 00-2-2H5a2 2 0 00-2 2v4a2 2 0 002 2h2m2 4h6a2 2 0 002-2v-4a2 2 0 00-2-2H9a2 2 0 00-2 2v4a2 2 0 002 2zm8-12V5a2 2 0 00-2-2H9a2 2 0 00-2 2v4h10z" />
            </svg>
            Print
          </button>
          {currentTicket.status === 'ACTIVE' && (
            <button
              onClick={() => setShowCancelModal(true)}
              className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
            >
              Cancel Ticket
            </button>
          )}
        </div>

        {/* Ticket Card */}
        <div
          ref={ticketRef}
          className="bg-white rounded-lg shadow-lg overflow-hidden border-2 border-gray-200"
        >
          {/* Ticket Header */}
          <div className="bg-gradient-to-r from-blue-600 to-blue-800 text-white p-6">
            <div className="flex justify-between items-start">
              <div>
                <h1 className="text-3xl font-bold mb-2">{currentTicket.eventName}</h1>
                <p className="text-blue-100 text-lg">
                  {currentTicket.eventDate && formatDate(currentTicket.eventDate)}
                </p>
              </div>
              <span
                className={`px-4 py-2 rounded-full text-sm font-semibold border-2 ${getStatusColor(
                  currentTicket.status
                )}`}
              >
                {currentTicket.status}
              </span>
            </div>
          </div>

          {/* Ticket Body */}
          <div className="p-6">
            <div className="grid md:grid-cols-2 gap-8">
              {/* Left Column - Details */}
              <div className="space-y-6">
                <div>
                  <h3 className="text-sm font-semibold text-gray-500 uppercase mb-2">
                    Ticket Information
                  </h3>
                  <div className="space-y-3">
                    <div>
                      <p className="text-sm text-gray-600">Ticket Number</p>
                      <p className="text-lg font-mono font-bold">{currentTicket.ticketNumber}</p>
                    </div>
                    <div>
                      <p className="text-sm text-gray-600">Ticket Type</p>
                      <p className="text-lg font-semibold">{currentTicket.ticketTypeName || 'General Admission'}</p>
                    </div>
                    {currentTicket.venueZone && (
                      <div>
                        <p className="text-sm text-gray-600">Zone/Section</p>
                        <p className="text-lg font-semibold">{currentTicket.venueZone}</p>
                      </div>
                    )}
                    <div>
                      <p className="text-sm text-gray-600">Ticket Holder</p>
                      <p className="text-lg font-semibold">{currentTicket.holderName}</p>
                    </div>
                  </div>
                </div>

                <div>
                  <h3 className="text-sm font-semibold text-gray-500 uppercase mb-2">
                    Venue Information
                  </h3>
                  <div className="space-y-2">
                    <div>
                      <p className="text-sm text-gray-600">Venue</p>
                      <p className="text-lg font-semibold">{currentTicket.venueName}</p>
                    </div>
                    {currentTicket.venueAddress && (
                      <div>
                        <p className="text-sm text-gray-600">Address</p>
                        <p className="text-base">{currentTicket.venueAddress}</p>
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* Right Column - QR Code */}
              <div className="flex flex-col items-center justify-center">
                <div className="bg-white p-4 rounded-lg border-4 border-gray-200">
                  {qrCodeUrl ? (
                    <img
                      src={qrCodeUrl}
                      alt="Ticket QR Code"
                      className="w-64 h-64"
                    />
                  ) : (
                    <div className="w-64 h-64 flex items-center justify-center bg-gray-100">
                      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
                    </div>
                  )}
                </div>
                <p className="mt-4 text-center text-sm text-gray-600">
                  Scan this QR code at the event entrance
                </p>
              </div>
            </div>
          </div>

          {/* Ticket Footer */}
          <div className="bg-gray-50 px-6 py-4 border-t">
            <div className="flex flex-col sm:flex-row justify-between items-center gap-4 text-sm text-gray-600">
              <div>
                <p>Order ID: {currentTicket.orderId}</p>
              </div>
              <div className="text-center sm:text-right">
                <p>Issued: {formatDate(currentTicket.createdAt)}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Important Information */}
        <div className="mt-6 bg-yellow-50 border border-yellow-200 rounded-lg p-6 print:hidden">
          <h3 className="font-semibold text-yellow-900 mb-3">Important Information</h3>
          <ul className="space-y-2 text-yellow-800 text-sm">
            <li className="flex items-start gap-2">
              <span className="text-yellow-600 mt-0.5">•</span>
              <span>Please arrive at least 30 minutes before the event starts</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-yellow-600 mt-0.5">•</span>
              <span>This ticket is valid for one person only</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-yellow-600 mt-0.5">•</span>
              <span>Present this QR code at the entrance for scanning</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-yellow-600 mt-0.5">•</span>
              <span>Screenshots or printed copies are acceptable</span>
            </li>
            <li className="flex items-start gap-2">
              <span className="text-yellow-600 mt-0.5">•</span>
              <span>Do not share your QR code with others to prevent unauthorized use</span>
            </li>
          </ul>
        </div>
      </div>

      {/* Cancel Confirmation Modal */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-lg max-w-md w-full p-6">
            <h3 className="text-xl font-bold mb-4">Cancel Ticket?</h3>
            <p className="text-gray-600 mb-6">
              Are you sure you want to cancel this ticket? This action cannot be undone.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowCancelModal(false)}
                className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition"
              >
                Keep Ticket
              </button>
              <button
                onClick={handleCancelTicket}
                className="flex-1 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                Cancel Ticket
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TicketDetailsPage;
