package com.globaltcad.swingtree;

class Settings {

	private ThreadMode threadMode = ThreadMode.COUPLED;

	public ThreadMode getThreadMode() {
		return threadMode;
	}

	public void setThreadMode(ThreadMode threadMode) {
		this.threadMode = threadMode;
	}

}
