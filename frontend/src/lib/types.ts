export type Role = 'CUSTOMER' | 'MERCHANT'
export type VehicleType = 'BIKE' | 'CAR'
export type ServiceCategory = 'WATER_WASH' | 'CLEANING' | 'FULL_SERVICE' | 'PUNCTURE' | 'TYRE' | 'OIL_CHANGE' | 'BATTERY'
export type BookingStatus = 'REQUESTED' | 'ACCEPTED' | 'DECLINED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED'
export type Day = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'

export interface Me {
  id: number; role: Role | null; name?: string; phone?: string; email?: string
  address?: string; pincode?: string; merchantOnboarded?: boolean | null
}
export interface Photo { id: number; url: string }
export interface MerchantProfile {
  id: number; shopName?: string; phone?: string; address?: string; pincode?: string; city?: string
  latitude?: number; longitude?: number; servesBike: boolean; servesCar: boolean; openDays: Day[]
  openTime: string; closeTime: string; slotMinutes: number; online: boolean; onboarded: boolean
  ratingAvg: number; ratingCount: number; photos: Photo[]
}
export interface ServiceItem {
  id: number; category: ServiceCategory; vehicleType: VehicleType; name: string; description?: string
  price: number; durationMin: number; imageUrl?: string; active: boolean
}
export interface MerchantSummary {
  id: number; shopName: string; address: string; pincode: string; ratingAvg: number; ratingCount: number
  fromPrice?: number; categories: ServiceCategory[]; coverPhoto?: string; openNow: boolean; distanceKm?: number
  servesBike: boolean; servesCar: boolean
}
export interface MerchantDetail { shop: MerchantProfile; services: ServiceItem[]; openNow: boolean; distanceKm?: number }
export interface Slot { time: string; available: boolean }
export interface SlotsResponse { date: string; open: boolean; slots: Slot[] }
export interface CustomerBooking {
  id: number; code: string; merchantId: number; shopName: string; shopPhone?: string; shopAddress?: string
  shopLatitude?: number; shopLongitude?: number; serviceName: string; vehicleType: VehicleType; price: number
  slotStart: string; status: BookingStatus; otp?: string; myRating?: number; createdAt: string
}
export interface MerchantBooking {
  id: number; code: string; serviceName: string; vehicleType: VehicleType; price: number; slotStart: string
  status: BookingStatus; customerName?: string; customerPincode: string; customerPhone?: string
  customerAddress?: string; createdAt: string
}
export interface Review {
  id: number; customerName: string; rating: number; comment?: string; reply?: string
  serviceName: string; vehicleType: VehicleType; createdAt: string
}
export interface ReviewSummary { ratingAvg: number; ratingCount: number; distribution: Record<string, number>; reviews: Review[] }
export interface AppNotification { id: number; type: string; title: string; body: string; bookingId?: number; read: boolean; createdAt: string }
export interface NotificationList { unread: number; items: AppNotification[] }
