#!/bin/bash
# Setup script for Flang NNUE training

echo "=== Flang NNUE Training Setup ==="
echo

# Check if venv exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
else
    echo "Virtual environment already exists"
fi

# Activate venv
echo "Activating virtual environment..."
source venv/bin/activate

# Install requirements
echo "Installing dependencies..."
pip install --upgrade pip
pip install -r requirements.txt

echo
echo "=== Setup Complete ==="
echo
echo "To activate the environment, run:"
echo "  source venv/bin/activate"
echo
echo "To start training, run:"
echo "  python train.py --dataset ../doc/eval_dataset_4.txt --epochs 100"
echo