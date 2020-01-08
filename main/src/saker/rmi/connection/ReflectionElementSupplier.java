package saker.rmi.connection;

import saker.rmi.exception.RMIObjectTransferFailureException;

interface ReflectionElementSupplier<T> {
	public T get() throws RMIObjectTransferFailureException;

	@Override
	public int hashCode();

	@Override
	public boolean equals(Object obj);

	public static <T> ReflectionElementSupplier<T> val(T val) {
		return new ValReflectionElementSupplier<>(val);
	}

	class ValReflectionElementSupplier<T> implements ReflectionElementSupplier<T> {
		private T value;

		public ValReflectionElementSupplier(T value) {
			this.value = value;
		}

		@Override
		public T get() throws RMIObjectTransferFailureException {
			return value;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			ValReflectionElementSupplier<?> other = (ValReflectionElementSupplier<?>) obj;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "[" + value + "]";
		}
	}
}
