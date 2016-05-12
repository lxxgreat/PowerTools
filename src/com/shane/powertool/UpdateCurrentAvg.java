package com.shane.powertool;

public class UpdateCurrentAvg extends CurrentAvg{
    int mLastCurrentAvg;
    
    public UpdateCurrentAvg(int size) {
        super(size);
        mLastCurrentAvg = 0;
    }
    
    public int getAvgCurrent() {
        if (mCurrentPos%mSize == 1) {
            mLastCurrentAvg = super.getAvgCurrent();
        }
        return mLastCurrentAvg;
    }
}
