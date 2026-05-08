"""
Evaluate a single FBN2 position using the trained NNUE model.

Usage:
    python evaluate_position.py --model models/best_model.pt --position "+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2"
"""

import torch
import argparse
from model import FlangNNUE
from board_parser import parse_position


def evaluate_position(model_path, fbn2_position):
    """
    Evaluate a single position using the trained model.

    Args:
        model_path: Path to the trained model checkpoint
        fbn2_position: FBN2 notation string

    Returns:
        float: Evaluation score
    """
    # Load model
    print(f"Loading model from: {model_path}")
    checkpoint = torch.load(model_path, map_location='cpu')

    # Create model with same architecture
    hidden_sizes = checkpoint.get('hidden_sizes', [128, 32, 16])
    model = FlangNNUE(hidden_sizes=hidden_sizes)
    model.load_state_dict(checkpoint['model_state_dict'])
    model.eval()

    print(f"Model loaded successfully")
    print(f"  Epoch: {checkpoint.get('epoch', 'unknown')}")
    print(f"  Hidden sizes: {hidden_sizes}")
    print()

    # Parse position into features
    print(f"Position: {fbn2_position}")
    features = parse_position(fbn2_position)

    # Convert to tensor and add batch dimension
    features_tensor = torch.from_numpy(features).unsqueeze(0)

    # Evaluate
    with torch.no_grad():
        output = model(features_tensor)
        evaluation = output.item()

    print(f"Evaluation: {evaluation:.6f}")
    print()

    # Show feature statistics
    import numpy as np
    print("Feature statistics:")
    print(f"  Shape: {features.shape}")
    print(f"  Non-zero elements: {np.count_nonzero(features)}")
    print(f"  Min: {features.min():.6f}")
    print(f"  Max: {features.max():.6f}")
    print(f"  Mean: {features.mean():.6f}")

    return evaluation


def main():
    parser = argparse.ArgumentParser(description='Evaluate a single FBN2 position')
    parser.add_argument('--model', type=str, default='models/best_model.pt',
                        help='Path to trained model checkpoint')
    parser.add_argument('--position', type=str,
                        default='+2PRHFUK2PPPPPP32ppp-ppp2kufhrp2',
                        help='FBN2 position to evaluate')

    args = parser.parse_args()

    print("=== NNUE Position Evaluation ===\n")

    evaluation = evaluate_position(args.model, args.position)

    print("\n=== Complete ===")
    return evaluation


if __name__ == "__main__":
    main()
