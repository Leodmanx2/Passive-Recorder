# Passive Recorder

Passive Recorder in an Android app that constantly records audio to a buffer
in memory. Any segment of that audio can be retroactively saved.

This project was started as an alternative to
[Echo](https://f-droid.org/en/packages/eu.mrogalski.saidit/). I had three
significant problems with Echo that I wanted to address. The biggest issue was
that I could not access my phone's FM tuner while it was recording. As it
turned out, this is due to a limitation of Android and Passive Recorder has
the same issue. The second issue was that Echo's UI was often unresponsive for
sometimes minutes at a time. There are a couple reasons this could be, but I
have not delved deeper. Regardless, I have taken care to avoid blocking with
Passive Recorder and it responds much better. The third issue I had with Echo
was its memory usage; it buffers raw audio samples. Passive Recorder buffers
AAC packets. Given the default (and currently non-configurable) settings,
Passive Recorder uses less than a fifth of the memory while maintaining "low"
battery usage. (That's what Android Studio reports, anyway. I have not run my
own benchmarks.)

Development is on indefinite hiatus now that I know the FM tuner problem can't
be worked around. Features that were on the roadmap which I have not
implemented include: a waveform/spectrogram view, selection using absolute
timestamps (as opposed to timestamps relative to the present), and playing
back the selection without saving it to persistent storage first.

## Installation

The application has not yet been pushed to any app repository, although it is
ready for use.

A pre-built application package is available
[here](https://bitbucket.org/leodmanx2/passive-recorder/downloads/Passive_Recorder.apk).
You will need to enable installing apps from untrusted sources in your
device's settings.

## License

Copyright © 2020 by Christopher MacLeod

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.

