# ThreeDSoundEngine — A Novel 3D Audio Engine for Headphones and Loudspeakers

ThreeDSoundEngine is a new **3D audio rendering engine** that creates the illusion of sound sources moving in real three‑dimensional space.

Unlike many binaural systems that work only over headphones, this engine is designed to be **effective over both headphones *and* loudspeakers** — giving a convincing sense of direction and height in a standard stereo setup.

Headphone demo available on YouTube [here](https://youtu.be/a941QokUL3I).  
Loudspeaker demo available on YouTube [here](https://youtu.be/1aBVKUXOofg).

---

## ✨ Key Features

- **3D spatialisation**  
  Positions sources anywhere around the listener (azimuth, elevation, distance).
  
- **Headphone *and* loudspeaker support**  
  Algorithms tuned so spatial cues survive stereo speaker playback.
  
- **Novelty**  
  Maintains strong localisation on speakers while preserving quality on headphones.
  
- **Automatically saves output**  
  Saves the output to an 'output.wav' file for later playback.
  
- **Maven‑based build**  
  Cross‑platform setup using the Maven Wrapper; no global Maven install required.
  
- **Dual‑licensed**  
  Open source under **GNU GPL v3**, with a separate **commercial license** available.

---

## 🚀 Quick Start

### Requirements
- Java 17+ (developed on JDK 21; compiled for Java SE 17 compatibility)
- Maven Wrapper (included: `mvnw` / `mvnw.cmd`)

### Clone & Build
```bash
git clone https://github.com/jamesstanier/ThreeDSoundEngine.git
cd ThreeDSoundEngine

# Linux/macOS
./mvnw clean package

# Windows
mvnw.cmd clean package
```

### Run

```bash
# Linux/macOS
./mvnw exec:java -Dexec.mainClass=threedsoundengine.Main

# Windows (PowerShell-safe syntax)
mvnw.cmd --% exec:java -Dexec.mainClass=threedsoundengine.Main
```
Or just double-click the 'ThreeDSoundEngine-0.0.1.jar' file (after downloading) to get it running.

---

## 🔬 How It Works (High Level)

The engine combines:
- **HRTF-style filtering** for directional cues.
- **Crosstalk-aware tricks** so spatialisation remains convincing on stereo loudspeakers.
- **Height rendering** via elevation‑dependent filtering.

These components aim to preserve localisation on headphones while **remaining effective on loudspeakers**, which is the key novelty. You just need to enhance the vertical localisation cues via the 'Vertical Gain' control (not too much); leave off for headphones.

The other controls are for varying the yellow test sound source movement in angular rates via the Azimuth and Polar sliders. The last three controls (pan divisions and upper and lower cut-off frequencies) are for front-back filtering, which requires more work.

---

## 🤝 Help Wanted

This project is early and I’d love community input—especially on:

- **Front–back discrimination**  
  Improve spectral/temporal cues that prevent “front/back flips.”
- **Overhead accuracy**  
  Better elevation cues for sources moving *above* the listener.
- **General DSP performance & tuning**  
  Efficient filter designs, parameter smoothing, and stability.

Please open issues/PRs with experiments, references, or datasets. Benchmarks and listening tests are hugely appreciated.

---

## 📜 License

This project is **dual‑licensed**:

- **GPL v3** (see [LICENSE](LICENSE)) — you may use, modify, and distribute under GPL v3.
- **Commercial license** — for proprietary/closed‑source use, contact me: j.stanier766(at)gmail.com.

---

## 🙏 Acknowledgements

- Informed by decades of research on an echolocation aid for the blind.
- Built with Java; often paired with libraries like Apache Commons (Geometry/Numbers) for math support.

---

## 🛣️ Roadmap

- Improve front–back filtering and overhead localisation
- Add presets for common speaker setups and listening rooms
- Provide audio demos and measurement scripts
- Enhanced GUI tools for interactive spatialisation
- Integration with the JUCE framework
