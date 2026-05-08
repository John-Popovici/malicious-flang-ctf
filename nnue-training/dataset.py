"""
Dataset loader for Flang NNUE training.

Loads positions from the evaluation dataset (FBN2|score format).
"""

import torch
from torch.utils.data import Dataset, DataLoader
import numpy as np
import math
from board_parser import parse_position


def cp_to_winrate(cp):
    return math.tanh(cp / (200.0 + 0.47 * abs(cp)))


class FlangDataset(Dataset):
    """
    PyTorch dataset for Flang positions.

    Each line in the dataset file format: FBN2|score
    Example: +2PRHFUK2PPPPPP32pppppp2kufhrp2|0.0
    """

    def __init__(self, filename, max_positions=None):
        """
        Initialize the dataset.

        Args:
            filename: Path to the dataset file
            max_positions: Maximum number of positions to load (None for all)
        """
        self.positions = []
        self.scores = []

        print(f"Loading dataset from {filename}...")

        with open(filename, 'r') as f:
            for i, line in enumerate(f):
                if max_positions and i >= max_positions:
                    break

                line = line.strip()
                if not line:
                    continue

                try:
                    # Parse line: FBN2|score
                    fbn2, score_str = line.split('|')
                    score = float(score_str)

                    # Convert to side-to-move perspective before winrate conversion
                    # FBN2 starts with '+' (white to move) or '-' (black to move)
                    if fbn2[0] == '-':
                        score = -score

                    # Convert CP to winrate [-1, 1]
                    score = cp_to_winrate(score)

                    # Parse position into features
                    features = parse_position(fbn2)

                    self.positions.append(features)
                    self.scores.append(score)

                except Exception as e:
                    print(f"Error parsing line {i}: {line[:50]}... - {e}")
                    continue

                if (i + 1) % 10000 == 0:
                    print(f"  Loaded {i + 1} positions...")

        print(f"Loaded {len(self.positions)} positions total")

        # Convert to numpy arrays for efficiency
        self.positions = np.array(self.positions, dtype=np.int8)
        self.scores = np.array(self.scores, dtype=np.float32)

        # Print dataset statistics
        print(f"\nDataset statistics:")
        print(f"  Mean score: {self.scores.mean():.3f}")
        print(f"  Std score: {self.scores.std():.3f}")
        print(f"  Min score: {self.scores.min():.3f}")
        print(f"  Max score: {self.scores.max():.3f}")

    def __len__(self):
        return len(self.positions)

    def __getitem__(self, idx):
        """
        Get a single training example.

        Returns:
            tuple: (features, score)
        """
        position = torch.from_numpy(self.positions[idx]).float()
        score = torch.tensor(self.scores[idx], dtype=torch.float32)
        return position, score


def create_dataloaders(dataset_file, batch_size=256, train_split=0.95, max_positions=None):
    """
    Create train and validation dataloaders.

    Args:
        dataset_file: Path to the dataset file
        batch_size: Batch size for training
        train_split: Fraction of data to use for training
        max_positions: Maximum positions to load (None for all)

    Returns:
        tuple: (train_loader, val_loader, dataset_size)
    """
    # Load full dataset
    dataset = FlangDataset(dataset_file, max_positions=max_positions)

    # Split into train and validation with fixed seed for reproducibility
    train_size = int(len(dataset) * train_split)
    val_size = len(dataset) - train_size

    # Use fixed seed to ensure same train/val split across runs
    # This prevents validation data from leaking into training when resuming
    generator = torch.Generator().manual_seed(42)
    train_dataset, val_dataset = torch.utils.data.random_split(
        dataset, [train_size, val_size], generator=generator
    )

    # Create dataloaders
    train_loader = DataLoader(
        train_dataset,
        batch_size=batch_size,
        shuffle=True,
        num_workers=4,
        pin_memory=True
    )

    val_loader = DataLoader(
        val_dataset,
        batch_size=batch_size,
        shuffle=False,
        num_workers=4,
        pin_memory=True
    )

    print(f"\nDataset split:")
    print(f"  Training: {train_size} positions")
    print(f"  Validation: {val_size} positions")

    return train_loader, val_loader, len(dataset)


if __name__ == "__main__":
    # Test dataset loading
    import sys

    if len(sys.argv) > 1:
        dataset_file = sys.argv[1]
    else:
        dataset_file = "../doc/eval_dataset_4.txt"

    print(f"Testing dataset loader with: {dataset_file}\n")

    # Load a small subset for testing
    train_loader, val_loader, total_size = create_dataloaders(
        dataset_file,
        batch_size=32,
        max_positions=1000
    )

    print(f"\nTesting batch loading...")
    for features, scores in train_loader:
        print(f"Batch features shape: {features.shape}")
        print(f"Batch scores shape: {scores.shape}")
        print(f"Sample scores: {scores[:5]}")
        break