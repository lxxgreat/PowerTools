package com.shane.powertool;

public class CurrentAvg {
    protected int mSize;
    protected int[] mCurrentNowArray;
    protected long mCurrentPos;
    
    public CurrentAvg(int size) {
        mCurrentPos = 0;
        if (size < 1) {
            throw new IllegalArgumentException("size: " + size + " should be positive number!");
        }
        mSize = size;
        mCurrentNowArray = new int[mSize];
       for (int i = 0; i < size; i++) {
           mCurrentNowArray[i] = 0;
       }
    }
    
    public void add(int current) {
        int index = (int)((mCurrentPos++)%mSize);
        mCurrentNowArray[index] = current;
        if (mCurrentPos > 2*mSize) {
            mCurrentPos -= mSize;
        }
    }
    
    public int getAvgCurrent() {
        if (mCurrentPos == 0) {
            return 0;
        }
        long sum = 0;
        int avg = 0;
        if (mCurrentPos < mSize) {
            for (int i = 0; i < mCurrentPos; i++) {
                sum += mCurrentNowArray[i];
            }
            avg = (int)(sum/mCurrentPos);
        } else {
            for (int i = 0; i < mSize; i++) {
                sum += mCurrentNowArray[i];
            }
            avg = (int)(sum/mSize);
        }
        
        return avg;
    }
}
