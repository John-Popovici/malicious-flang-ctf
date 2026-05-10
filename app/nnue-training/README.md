# Flang NNUE Training

PyTorch training pipeline for NNUE (Efficiently Updatable Neural Network) evaluation for the Flang chess variant.

## Setup

1. Create and activate virtual environment:
```bash
python3 -m venv venv
source venv/bin/activate  # On Linux/Mac
# or
venv\Scripts\activate  # On Windows
```

2. Install dependencies:
```bash
pip install -r requirements.txt
```

## Dataset Format

The training expects a dataset file with format: `FBN2|score`

Example:
```
+2PRHFUK2PPPPPP32pppppp2kufhrp2|0.0
-2PRHFUK2PPPPP7P-25pppppp2kufhrp2|5.528
```

Where:
- FBN2 is the board notation (+ for white to move, - for black)
- Score is the evaluation from deep search

## Training

Basic training:
```bash
python train.py --dataset ../doc/eval_dataset_4.txt --epochs 100
```

With custom parameters:
```bash
python train.py \
  --dataset ../doc/eval_dataset_4.txt \
  --epochs 100 \
  --batch-size 256 \
  --lr 0.001 \
  --hidden-sizes 256 128 64 \
  --output-dir models
```

Parameters:
- `--dataset`: Path to training dataset
- `--epochs`: Number of training epochs (default: 100)
- `--batch-size`: Training batch size (default: 256)
- `--lr`: Learning rate (default: 0.001)
- `--hidden-sizes`: Hidden layer sizes (default: 256 128 64)
- `--max-positions`: Limit number of positions (for testing)
- `--output-dir`: Directory to save models (default: models)
- `--log-dir`: TensorBoard log directory (default: runs)

## Monitoring Training

View training progress with TensorBoard:
```bash
tensorboard --logdir runs
```

Then open http://localhost:6006 in your browser.

## Model Architecture

The NNUE uses:
- **Input**: 896 features (14 planes × 8 × 8)
  - 12 piece planes (6 piece types × 2 colors)
  - 1 frozen pieces plane
  - 1 side-to-move plane
- **Hidden layers**: Configurable (default: 256 → 128 → 64)
- **Activation**: SCReLU (Squared Clipped ReLU)
- **Output**: Single evaluation score

SCReLU: `f(x) = clamp(x, 0, 1)²`

This is the standard activation used in chess NNUE networks.

## Export Model

Export trained model for C/C++ integration:

```bash
# Export to binary format
python export.py --model models/best_model.pt --format binary --output flang_nnue

# Export to C header
    python export.py --model models/best_model.pt --format c_header --output flang_nnue

# Export both formats
python export.py --model models/best_model.pt --format both --output flang_nnue
```

This creates:
- `flang_nnue.nnue`: Binary format for runtime loading
- `flang_nnue.h`: C header with embedded weights

## Testing Components

Test the board parser:
```bash
python board_parser.py
```

Test the model:
```bash
python model.py
```

Test the dataset loader:
```bash
python dataset.py ../doc/eval_dataset_4.txt
```

## Project Structure

```
nnue-training/
├── board_parser.py    # FBN2 notation parser
├── model.py          # NNUE model architecture
├── dataset.py        # Dataset loader
├── train.py          # Training script
├── export.py         # Model export utilities
├── requirements.txt  # Python dependencies
└── README.md         # This file
```

## Integration with C Engine

After training and exporting, integrate the NNUE into your C engine:

1. **Binary format**: Load `flang_nnue.nnue` at runtime
2. **C header**: Include `flang_nnue.h` and compile weights directly

You'll need to implement:
- FBN2 → feature extraction (matching board_parser.py)
- SCReLU activation: `clamp(x, 0, 1)²`
- Matrix multiplication for forward pass
- Optionally: SIMD optimizations for SCReLU

## Tips

- Start with a small subset to verify training works:
  ```bash
  python train.py --dataset ../doc/eval_dataset_4.txt --max-positions 10000 --epochs 10
  ```

- Monitor for overfitting - validation loss should track training loss

- If training is slow, reduce `--batch-size` or `--hidden-sizes`

- The model learns fastest in the first ~20 epochs

- Consider data augmentation by flipping board horizontally/vertically