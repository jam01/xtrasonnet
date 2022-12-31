local lib3 = import 'lib-3.libsonnet';

{
    xtr(val): xtr.toLowerCase(val),
    std(val): std.asciiLower(val),
    echo(val): val,
    lib3: {
        xtr: lib3.xtr('Hello'),
        std: lib3.std('Hello'),
        echo: lib3.echo('Hello')
    }
}