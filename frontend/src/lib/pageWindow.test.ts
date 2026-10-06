import { describe, expect, it } from 'vitest'
import { pageWindow } from './pageWindow'

describe('pageWindow', () => {
  it('shows every page when there are few', () => {
    expect(pageWindow(0, 5)).toEqual([0, 1, 2, 3, 4])
  })
  it('collapses the middle with ellipses', () => {
    expect(pageWindow(5, 20)).toEqual([0, '…', 4, 5, 6, '…', 19])
    expect(pageWindow(0, 20)).toEqual([0, 1, '…', 19])
    expect(pageWindow(19, 20)).toEqual([0, '…', 18, 19])
  })
})
