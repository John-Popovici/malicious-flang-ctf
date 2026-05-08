"""
Training script for Flang NNUE model.

Usage:
    # Train from scratch
    python train.py --dataset ../doc/eval_dataset_4.txt --epochs 100

    # Resume training from checkpoint
    python train.py --dataset ../doc/eval_dataset_4.txt --epochs 100 --resume models/best_model.pt

    # Resume with new learning rate
    python train.py --dataset ../doc/eval_dataset_4.txt --epochs 50 --resume models/checkpoint_epoch_500.pt --lr 0.0001

    # Resume with original learning rate from checkpoint
    python train.py --dataset ../doc/eval_dataset_4.txt --epochs 50 --resume models/best_model.pt --resume-lr
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.tensorboard import SummaryWriter
import argparse
from pathlib import Path
from tqdm import tqdm
import time

from model import FlangNNUE
from dataset import create_dataloaders


def train_epoch(model, train_loader, criterion, optimizer, device, epoch):
    """Train for one epoch."""
    model.train()
    total_loss = 0.0
    num_batches = 0

    progress_bar = tqdm(train_loader, desc=f"Epoch {epoch}")

    for features, scores in progress_bar:
        # Move to device
        features = features.to(device)
        scores = scores.to(device).unsqueeze(1)  # Add dimension for batch

        # Forward pass
        optimizer.zero_grad()
        predictions = model(features)

        # Compute loss
        loss = criterion(predictions, scores)

        # Backward pass
        loss.backward()
        optimizer.step()

        # Track statistics
        total_loss += loss.item()
        num_batches += 1

        # Update progress bar
        progress_bar.set_postfix({'loss': f'{loss.item():.4f}'})

    avg_loss = total_loss / num_batches
    return avg_loss


def validate(model, val_loader, criterion, device):
    """Validate the model."""
    model.eval()
    total_loss = 0.0
    num_batches = 0

    with torch.no_grad():
        for features, scores in val_loader:
            # Move to device
            features = features.to(device)
            scores = scores.to(device).unsqueeze(1)

            # Forward pass
            predictions = model(features)

            # Compute loss
            loss = criterion(predictions, scores)

            total_loss += loss.item()
            num_batches += 1

    avg_loss = total_loss / num_batches
    return avg_loss


def main():
    parser = argparse.ArgumentParser(description='Train Flang NNUE model')
    parser.add_argument('--dataset', type=str, default='../doc/eval_dataset_5_unique.txt',
                        help='Path to dataset file')
    parser.add_argument('--epochs', type=int, default=30,
                        help='Number of training epochs')
    parser.add_argument('--batch-size', type=int, default=4096,
                        help='Batch size for training')
    parser.add_argument('--lr', type=float, default=0.001,
                        help='Learning rate')
    parser.add_argument('--max-positions', type=int, default=None,
                        help='Maximum positions to load (None for all)')
    parser.add_argument('--hidden-sizes', type=int, nargs='+', default=[128, 32],
                        help='Hidden layer sizes')
    parser.add_argument('--output-dir', type=str, default='models',
                        help='Directory to save models')
    parser.add_argument('--log-dir', type=str, default='runs',
                        help='Directory for tensorboard logs')
    parser.add_argument('--resume', type=str, default=None,
                        help='Path to checkpoint to resume training from')
    parser.add_argument('--resume-lr', action='store_true',
                        help='Resume with the learning rate from checkpoint (otherwise uses --lr)')

    args = parser.parse_args()

    # Setup
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}")

    # Create output directory
    output_dir = Path(args.output_dir)
    output_dir.mkdir(exist_ok=True)

    # Create dataloaders
    print("\n=== Loading Dataset ===")
    train_loader, val_loader, total_size = create_dataloaders(
        args.dataset,
        batch_size=args.batch_size,
        max_positions=args.max_positions
    )

    # Create model
    print("\n=== Creating Model ===")
    start_epoch = 1

    if args.resume:
        # Load checkpoint
        print(f"Loading checkpoint from: {args.resume}")
        checkpoint = torch.load(args.resume, map_location=device)

        # Create model with architecture from checkpoint
        hidden_sizes = checkpoint.get('hidden_sizes', args.hidden_sizes)
        model = FlangNNUE(hidden_sizes=hidden_sizes).to(device)
        model.load_state_dict(checkpoint['model_state_dict'])

        # Get starting epoch
        start_epoch = checkpoint.get('epoch', 0) + 1

        print(f"Resumed from epoch {checkpoint.get('epoch', 0)}")
        print(f"Previous train loss: {checkpoint.get('train_loss', 'N/A'):.6f}")
        print(f"Previous val loss: {checkpoint.get('val_loss', 'N/A'):.6f}")
    else:
        # Create new model
        model = FlangNNUE(hidden_sizes=args.hidden_sizes).to(device)

    print(model)
    total_params = sum(p.numel() for p in model.parameters())
    print(f"Total parameters: {total_params:,}")

    # Loss and optimizer
    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=args.lr, weight_decay=1e-4)

    # Load optimizer state if resuming and requested
    if args.resume and args.resume_lr:
        optimizer.load_state_dict(checkpoint['optimizer_state_dict'])
        print(f"Resumed optimizer state (LR: {optimizer.param_groups[0]['lr']:.6f})")

    # Learning rate scheduler
    #scheduler = optim.lr_scheduler.ReduceLROnPlateau(
    #    optimizer, mode='min', factor=0.5, patience=5
    #)

    # Tensorboard writer
    writer = SummaryWriter(args.log_dir)

    # Training loop
    print("\n=== Training ===")
    best_val_loss = checkpoint.get('val_loss', float('inf')) if args.resume else float('inf')
    start_time = time.time()

    for epoch in range(start_epoch, start_epoch + args.epochs):
        # Train
        train_loss = train_epoch(model, train_loader, criterion, optimizer, device, epoch)

        # Validate
        val_loss = validate(model, val_loader, criterion, device)

        # Learning rate scheduling
        # scheduler.step(val_loss)

        # Log to tensorboard
        writer.add_scalar('Loss/train', train_loss, epoch)
        writer.add_scalar('Loss/val', val_loss, epoch)
        writer.add_scalar('LR', optimizer.param_groups[0]['lr'], epoch)

        # Print statistics
        elapsed = time.time() - start_time
        total_epochs = start_epoch + args.epochs - 1
        print(f"\nEpoch {epoch}/{total_epochs}")
        print(f"  Train Loss: {train_loss:.6f}")
        print(f"  Val Loss:   {val_loss:.6f}")
        print(f"  LR:         {optimizer.param_groups[0]['lr']:.6f}")
        print(f"  Time:       {elapsed/60:.1f}m")

        # Save best model
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            model_path = output_dir / 'best_model.pt'
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'train_loss': train_loss,
                'val_loss': val_loss,
                'hidden_sizes': args.hidden_sizes,
            }, model_path)
            print(f"  Saved best model to {model_path}")

        # Save checkpoint every 10 epochs
        if epoch % 10 == 0:
            checkpoint_path = output_dir / f'checkpoint_epoch_{epoch}.pt'
            torch.save({
                'epoch': epoch,
                'model_state_dict': model.state_dict(),
                'optimizer_state_dict': optimizer.state_dict(),
                'train_loss': train_loss,
                'val_loss': val_loss,
                'hidden_sizes': args.hidden_sizes,
            }, checkpoint_path)

    writer.close()

    print("\n=== Training Complete ===")
    print(f"Best validation loss: {best_val_loss:.6f}")
    print(f"Total training time: {(time.time() - start_time)/60:.1f}m")
    print(f"Models saved to: {output_dir}")


if __name__ == "__main__":
    main()