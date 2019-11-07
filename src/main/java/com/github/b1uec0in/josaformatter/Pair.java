package com.github.b1uec0in.josaformatter;

/**
 * Created by Bae Yong-Ju on 2017-05-30.
 */
public class Pair<FIRST, SECOND> {

    public FIRST first;
    public SECOND second;

    public Pair(FIRST first, SECOND second) {
        this.first = first;
        this.second = second;
    }

    @Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

    @Override
    public boolean equals(Object object) {
        if (object instanceof Pair) {
            Pair<?, ?> pair = (Pair<?, ?>) object;
            return this.first.equals(pair.first) &&
                    this.second.equals(pair.second);
        } else {
            return false;
        }
    }
}
