"""
NNUE (Efficiently Updatable Neural Network) model for Flang chess variant.

Architecture:
- Input: 896 features (14 planes × 8 × 8 board)
- Hidden layer 1: 128 neurons with SCReLU
- Hidden layer 2: 32 neurons with SCReLU
- Output: Single value (position evaluation, no activation)

SCReLU (Squared Clipped ReLU): f(x) = clamp(x, 0, 1)^2
Most prevalent in NNUE and produces the strongest networks.
"""

import torch
import torch.nn as nn


class SCReLU(nn.Module):
    """
    Squared Clipped ReLU activation function.
    f(x) = clamp(x, 0, 1)^2

    This is the standard activation function used in NNUE networks.
    """

    def forward(self, x):
        return torch.clamp(x, 0.0, 1.0).pow(2)


class FlangNNUE(nn.Module):
    """
    NNUE model for Flang position evaluation.
    """

    def __init__(self, input_size=896, hidden_sizes=[128, 32]):
        super(FlangNNUE, self).__init__()

        self.input_size = input_size
        self.hidden_sizes = hidden_sizes

        # Build network layers with SCReLU activation
        layers = []
        prev_size = input_size

        for hidden_size in hidden_sizes:
            layers.append(nn.Linear(prev_size, hidden_size))
            layers.append(SCReLU())
            prev_size = hidden_size

        # Output layer (no activation - raw evaluation score)
        layers.append(nn.Linear(prev_size, 1))

        self.network = nn.Sequential(*layers)

        # Initialize weights
        self._init_weights()

    def _init_weights(self):
        """Initialize network weights using Xavier initialization."""
        for module in self.modules():
            if isinstance(module, nn.Linear):
                nn.init.xavier_uniform_(module.weight)
                nn.init.zeros_(module.bias)

    def forward(self, x):
        """
        Forward pass through the network.

        Args:
            x: Input tensor of shape (batch_size, 14, 8, 8) or (batch_size, 896)

        Returns:
            Evaluation scores of shape (batch_size, 1)
        """
        # Flatten input if needed
        if len(x.shape) == 4:
            x = x.view(x.size(0), -1)

        return self.network(x)

    def predict(self, x):
        """
        Make predictions without gradients.

        Args:
            x: Input tensor

        Returns:
            Evaluation scores
        """
        self.eval()
        with torch.no_grad():
            return self.forward(x)


def create_model(device='cpu'):
    """
    Create and return a FlangNNUE model.

    Args:
        device: Device to place the model on ('cpu' or 'cuda')

    Returns:
        FlangNNUE model
    """
    model = FlangNNUE()
    model = model.to(device)
    return model


if __name__ == "__main__":
    # Test model creation
    model = create_model()
    print(f"Model architecture:\n{model}")

    # Test forward pass
    batch_size = 32
    test_input = torch.randn(batch_size, 14, 8, 8)
    output = model(test_input)
    print(f"\nInput shape: {test_input.shape}")
    print(f"Output shape: {output.shape}")

    # Count parameters
    total_params = sum(p.numel() for p in model.parameters())
    print(f"\nTotal parameters: {total_params:,}")