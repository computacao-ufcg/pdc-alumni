package br.edu.ufcg.computacao.alumni.core.models;

import java.util.Arrays;

public class ParsedName {

	private String[] names;
	private String[] surnames;
	private String suffix;
	
	public ParsedName(String[] names, String[] surnames, String suffix) {
		this.names = names;
		this.surnames = surnames;
		this.suffix = suffix;
	}

	public String[] getNames() {
		return names;
	}

	public void setNames(String[] names) {
		this.names = names;
	}

	public String[] getSurnames() {
		return surnames;
	}

	public void setSurnames(String[] surnames) {
		this.surnames = surnames;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(names);
		result = prime * result + ((suffix == null) ? 0 : suffix.hashCode());
		result = prime * result + Arrays.hashCode(surnames);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ParsedName other = (ParsedName) obj;
		if (!Arrays.equals(names, other.names))
			return false;
		if (suffix == null) {
			if (other.suffix != null)
				return false;
		} else if (!suffix.equals(other.suffix))
			return false;
		if (!Arrays.equals(surnames, other.surnames))
			return false;
		return true;
	}
	
}
