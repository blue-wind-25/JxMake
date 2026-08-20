public class T {
    public Boolean permits(Class<?> c) {
        return permits(c.getName());
    }

    public Boolean permits(String name) {
        return overrides.get(name);
    }
}
