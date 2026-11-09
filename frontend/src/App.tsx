import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './lib/auth'
import { Spinner } from './components/ui'
import Login from './pages/Login'
import RoleSelect from './pages/RoleSelect'
import Home from './pages/customer/Home'
import Shop from './pages/customer/Shop'
import Book from './pages/customer/Book'
import Bookings from './pages/customer/Bookings'
import Notifications from './pages/Notifications'
import Profile from './pages/customer/Profile'
import Onboarding from './pages/merchant/Onboarding'
import Orders from './pages/merchant/Orders'
import OrderDetail from './pages/merchant/OrderDetail'
import Services from './pages/merchant/Services'
import Reviews from './pages/merchant/Reviews'

export default function App() {
  const { loading, signedIn, me } = useAuth()

  if (loading) return <Spinner label="Starting…" />
  if (!signedIn || !me) return <Login />
  if (!me.role) return <RoleSelect />

  if (me.role === 'MERCHANT') {
    if (!me.merchantOnboarded) {
      return (
        <Routes>
          <Route path="*" element={<Onboarding first />} />
        </Routes>
      )
    }
    return (
      <Routes>
        <Route path="/merchant" element={<Orders />} />
        <Route path="/merchant/orders/:id" element={<OrderDetail />} />
        <Route path="/merchant/services" element={<Services />} />
        <Route path="/merchant/reviews" element={<Reviews />} />
        <Route path="/merchant/shop" element={<Onboarding />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="*" element={<Navigate to="/merchant" replace />} />
      </Routes>
    )
  }

  return (
    <Routes>
      <Route path="/" element={<Home />} />
      <Route path="/shop/:id" element={<Shop />} />
      <Route path="/book/:shopId" element={<Book />} />
      <Route path="/book/:shopId/:serviceId" element={<Book />} />
      <Route path="/bookings" element={<Bookings />} />
      <Route path="/notifications" element={<Notifications />} />
      <Route path="/profile" element={<Profile />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
