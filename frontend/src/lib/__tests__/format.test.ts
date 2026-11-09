import { describe, expect, it } from 'vitest'
import { dateStrip, duration, isValidMobile, isValidPincode, rupees, slotGroup, slotLabel, time12 } from '../format'

describe('format helpers', () => {
  it('builds a date strip and marks closed days', () => {
    // Monday 9 Nov 2026; shop closed on Sunday
    const strip = dateStrip(new Date(2026, 10, 9), 8, ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'])
    expect(strip).toHaveLength(8)
    expect(strip[0]).toMatchObject({ iso: '2026-11-09', dow: 'Today', day: 9, open: true })
    expect(strip[6]).toMatchObject({ iso: '2026-11-15', open: false })
    expect(strip[7].iso).toBe('2026-11-16')
  })

  it('crosses month boundaries', () => {
    const strip = dateStrip(new Date(2026, 10, 29), 3, ['SUNDAY', 'MONDAY', 'TUESDAY'])
    expect(strip.map((d) => d.iso)).toEqual(['2026-11-29', '2026-11-30', '2026-12-01'])
  })

  it('formats times and slots', () => {
    expect(time12('16:30:00')).toBe('4:30 PM')
    expect(time12('09:00')).toBe('9:00 AM')
    expect(time12('12:00')).toBe('12:00 PM')
    expect(time12('00:15')).toBe('12:15 AM')
    expect(slotLabel('2026-11-10T16:30:00')).toMatch(/Tue.*10.*Nov.*4:30 PM/)
  })

  it('groups slots by part of day', () => {
    expect(slotGroup('09:30:00')).toBe('Morning')
    expect(slotGroup('12:00:00')).toBe('Afternoon')
    expect(slotGroup('16:00:00')).toBe('Evening')
  })

  it('validates Indian pincode and mobile', () => {
    expect(isValidPincode('560034')).toBe(true)
    expect(isValidPincode('060034')).toBe(false)
    expect(isValidPincode('56003')).toBe(false)
    expect(isValidMobile('98765 43210')).toBe(true)
    expect(isValidMobile('1234567890')).toBe(false)
  })

  it('formats rupees', () => {
    expect(rupees(1199)).toBe('₹1,199')
    expect(rupees(149.5)).toBe('₹149.5')
  })

  it('formats durations', () => {
    expect(duration(50)).toBe('50 min')
    expect(duration(90)).toBe('1 h 30 min')
    expect(duration(180)).toBe('3 h')
  })
})
